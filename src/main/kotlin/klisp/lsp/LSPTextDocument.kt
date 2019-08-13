package klisp.lsp

import klisp.*
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

    private fun startDiagPull() = Unit
/*
        launch("pull-diags") {
            while (pulling) {
                val dr = chanDiags.receive()
                // TODO
            }
        }
*/

    private fun startChangePull() = launch("pull-chtrk") {
        while (pulling)
            procChange(chanChtrk.receive())
    }

    fun disconnectClient() {
        this.pulling = false
        this.client = null
    }

    fun connectClient(client: LanguageClient) {
        this.client = client
        this.pulling = true
        startChangePull()
        startDiagPull()
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
        val doc = params.textDocument
        docs.computeIfAbsent(doc.uri) { doc.text }
        launch("send-diags") { chanDiags.send(doc.uri to doc.text) }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        val doc = params.textDocument
        val txt = docs[doc.uri] ?: throw IllegalStateException("doc ${doc.uri} not found")
        launch("send-diags") { chanDiags.send(doc.uri to txt) }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        val doc = params.textDocument
        docs.remove(doc.uri)
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
                txt
            }
        }
        docs.computeIfPresent(doc.uri) { _, _ -> newTxt }
        launch("send-diags") { chanDiags.send(doc.uri to newTxt) }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        launch("send-chtrk") {
            chanChtrk.send(params)
        }
    }

    override fun completion(position: CompletionParams)
        : CompletableFuture<Either<MutableList<CompletionItem>, CompletionList>> = try {
        val std = stdEnv.entries.map { (s, e) ->
            val ci = CompletionItem(s.value)
            ci.documentation = Either.forLeft(e.docs)
            ci.kind = klType2lsp(e)
            ci
        }
        val spec = specialForm.values().map { sf ->
            val ci = CompletionItem(sf.name.toLowerCase())
            ci.documentation = Either.forLeft(sf.docs)
            ci.kind = CompletionItemKind.Keyword
            val ass = sf.aliases?.map { a ->
                val cit = CompletionItem(a)
                cit.documentation = ci.documentation
                cit.kind = ci.kind
                cit
            } ?: emptyList()
            ass + ci
        }.flatten()
        completedFuture(Either.forLeft((std + spec).toMutableList()))
    } catch (t: Throwable) {
        completedFuture(null)
    }

    private fun klType2lsp(type: Any): CompletionItemKind = when (type) {
        is func -> CompletionItemKind.Function
        is symbol -> CompletionItemKind.Constant
        is keyword -> CompletionItemKind.Keyword
        is string -> CompletionItemKind.Text
        is number<*> -> CompletionItemKind.Constant
        else -> throw IllegalArgumentException("unknown type <$type>")
    }

    override fun hover(position: TextDocumentPositionParams): CompletableFuture<Hover> {
        val txt = docs[position.textDocument.uri]
            ?: throw IllegalStateException("doc ${position.textDocument.uri} not found")
        val pos = position.position

        return try {
            val ctx = txt.toHoverCtx(pos)
            val docs = stdEnv
                .entries
                .firstOrNull { (s, _) -> s.value == ctx }
                ?.value
                ?: if (specialForm.isSpecial(ctx)) specialForm.from(ctx).docs else null
            completedFuture(Hover(MarkupContent(MarkupKind.MARKDOWN, if (docs !== null) docs.toString() else null)))
        } catch (t: Throwable) {
            completedFuture(null)
        }
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>> {
        val start = params.range.start
        val end = params.range.end
        val hasSelection = (end.character - start.character) > 1 || (end.line - start.line) != 0

//        LOGGER.debug("\nstart: $start, end: $end, hasSel: $hasSelection\n")

        return completedFuture(
            listOf(
                Either.forLeft(
                    Command(
                        if (hasSelection) "Evaluate selection" else "Evaluate file",
                        LSPWorkspace.EVAL,
                        listOf(
                            docs[params.textDocument.uri],
                            params.range
                        )
                    )
                )
            )
        )
    }
}