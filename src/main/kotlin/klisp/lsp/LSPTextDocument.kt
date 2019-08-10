package klisp.lsp

import klisp.LOGGER
import klisp.lsp
import klisp.specialForm
import klisp.stdEnv
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.TextDocumentService
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class LSPTextDocument : TextDocumentService {
    companion object {
        //        private val UNDEFINED_RANGE = Range(Position(), Position())
        private var TC = 0
    }

    private val docs: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    private val chanChtrk: Channel<DidChangeTextDocumentParams> = Channel(Channel.RENDEZVOUS)
    private val chanDiags: Channel<Pair<String, String>> = Channel(Channel.CONFLATED)
    private val disp = Executors.newFixedThreadPool(3) {
        val th = Thread(it)
        th.name = "disp ${TC++}"
        th
    }.asCoroutineDispatcher()

    @Volatile
    private var client: LanguageClient? = null
    @Volatile
    private var pulling = false

    private fun launch(name: String = "crt", fn: suspend () -> Unit): Job =
        CoroutineScope(disp + CoroutineName(name)).launch { fn() }

    private fun startDiagPull() = launch("pull-diags") {
        LOGGER.info { "lsp diagnostics pull started" }
        while (pulling) {
            val dr = chanDiags.receive()
            LOGGER.lsp { "diag pull $dr" }
        }
    }

    private fun startChangePull() = launch("pull-chtrk") {
        LOGGER.info { "lsp changes tracking pull started" }
        while (pulling) {
            val params = chanChtrk.receive()
            procChange(params)
            LOGGER.lsp { "chtrk pull $params" }
        }
    }

    fun disconnectClient() {
        this.pulling = false
        this.client = null
        LOGGER.info { "lsp client disconnected" }
    }

    fun connectClient(client: LanguageClient) {
        this.client = client
        this.pulling = true
        startChangePull()
        startDiagPull()
        LOGGER.info { "lsp client connected" }
    }

    private fun String.toAcCtx(pos: Position): String {
        val range = toRawPos(pos)
        val s = range.first
        val e = range.second - 1 // last is dot
        return this
            .substring(s, e)
            .reversed()
            .takeWhile(::isNotWordBoundary)
            .reversed()
    }

    private fun String.toHoverCtx(pos: Position): String {
        val (_, e) = toRawPos(pos)
        val lineLeft = this.substring(0, e).substringAfterLast('\n')
        val lineRight = this.substring(e).substringBefore('\n')
        val line = lineLeft + lineRight
        val wordLeft = line.substring(0, pos.character).reversed().takeWhile(::isNotWordBoundary).reversed()
        val wordRight = line.substring(pos.character).takeWhile(::isNotWordBoundary)
        return (wordLeft + wordRight)
            .replace("(", "")
            .replace(")", "")
    }

    private fun isNotWordBoundary(c: Char) = c !in listOf(' ', '.', ')', '\n')

    private fun String.toRawPos(pos: Position): Pair<Int, Int> {
        val reader = BufferedReader(StringReader(this))
        val writer = StringWriter()
        // Skip unchanged lines
        var line = 0
        while (line < pos.line) {
            writer.write(reader.readLine() + '\n')
            line++
        }
        val start = writer.toString().length
        return start to (start + pos.character)
    }

    private fun patch(sourceText: String, change: TextDocumentContentChangeEvent): String {
        val range = change.range
        val reader = BufferedReader(StringReader(sourceText))
        val writer = StringWriter()

        // Skip unchanged lines
        var line = 0
        while (line < range.start.line) {
            writer.write(reader.readLine() + '\n')
            line++
        }

        // Skip unchanged chars
        for (character in 0 until range.start.character)
            writer.write(reader.read())

        // Write replacement text
        writer.write(change.text)
        // Skip replaced text
        reader.skip(change.rangeLength.toLong())
        // Write remaining text
        while (true) {
            val next = reader.read()
            if (next == -1) return writer.toString()
            else writer.write(next)
        }
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        LOGGER.lsp { "open: $params" }
        val doc = params.textDocument
        docs.computeIfAbsent(doc.uri) { doc.text }
        launch("send-diags") { chanDiags.send(doc.uri to doc.text) }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        LOGGER.lsp { "save: $params" }
        val doc = params.textDocument
        val txt = docs[doc.uri] ?: throw IllegalStateException("doc ${doc.uri} not found")
        launch("send-diags") { chanDiags.send(doc.uri to txt) }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        LOGGER.lsp { "close: $params" }
        val doc = params.textDocument
        docs.remove(doc.uri)
        LOGGER.debug { "docs: $docs" }
    }

    private fun procChange(params: DidChangeTextDocumentParams) {
        val doc = params.textDocument
        val txt = docs[doc.uri] ?: throw IllegalStateException("doc ${doc.uri} not found")
        val newTxt = params.contentChanges.fold(txt) { acc: String, action: TextDocumentContentChangeEvent ->
            try {
                if (action.range === null && action.rangeLength === null)
                    action.text
                else
                    patch(acc, action)
            } catch (t: Throwable) {
                LOGGER.error { "edit failed: ${t.message}" }
                txt
            }
        }
        docs.computeIfPresent(doc.uri) { _, _ -> newTxt }
        LOGGER.lsp { "doc: ${doc.uri}, text:\n${newTxt.replace("\n", "|")}" }
        launch("send-diags") { chanDiags.send(doc.uri to newTxt) }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        launch("send-chtrk") {
            LOGGER.lsp { "change: $params" }
            chanChtrk.send(params)
        }
    }

    override fun completion(position: CompletionParams)
        : CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> {
        LOGGER.lsp { "complete: $position" }

        val txt = docs[position.textDocument.uri]
            ?: throw IllegalStateException("doc ${position.textDocument.uri} not found")
        val kind = position.context.triggerKind to position.context.triggerCharacter
        val pos = position.position

        return try {
            val ctx = txt.toAcCtx(pos)
            LOGGER.lsp { "doc: ${position.textDocument.uri}, kind: $kind, pos: $pos, ctx: $ctx" }

            completedFuture(Either.forLeft(mutableListOf()))
        } catch (t: Throwable) {
            LOGGER.warn { "complete failed: ${t.message}" }
            completedFuture(null)
        }
    }

    override fun hover(position: TextDocumentPositionParams): CompletableFuture<Hover> {
        LOGGER.lsp { "hover: $position" }

        val txt = docs[position.textDocument.uri]
            ?: throw IllegalStateException("doc ${position.textDocument.uri} not found")
        val pos = position.position

        return try {
            val ctx = txt.toHoverCtx(pos)
            LOGGER.lsp { "doc: ${position.textDocument.uri}, pos: $pos, ctx: $ctx" }
            val docs = stdEnv
                .entries
                .firstOrNull { (s, _) -> s.value == ctx }
                ?.key
                ?.docs
                ?: specialForm.from(ctx).docs
            completedFuture(Hover(MarkupContent(MarkupKind.MARKDOWN, docs)))
        } catch (t: Throwable) {
            LOGGER.warn { "hover failed: ${t.message}" }
            completedFuture(null)
        }
    }
}