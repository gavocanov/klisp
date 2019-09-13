package klisp.lsp

import com.google.gson.Gson
import com.google.gson.JsonElement
import klisp.err
import klisp.evaluate
import klisp.nok
import klisp.ok
import klisp.parser.hasBalancedRoundBrackets
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class LSPWorkspace : WorkspaceService, LanguageClientAware {
    companion object {
        const val EVAL = "eval"
        private val gson = Gson()
    }

    private lateinit var client: LanguageClient

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) = Unit
    override fun didChangeConfiguration(params: DidChangeConfigurationParams) = Unit

    override fun connect(client: LanguageClient) {
        this.client = client
    }


    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        val args = params.arguments
        when (params.command) {
            EVAL -> {
                val uri = gson.fromJson(args[0] as JsonElement, String::class.java)
                val content = gson.fromJson(args[1] as JsonElement, String::class.java)
                val range = gson.fromJson(args[2] as JsonElement, Range::class.java)
                val start = range.start
                val end = range.end
                val hasSelection =
                    (end.character - start.character) != 0 || (end.line - start.line) > 1

                val lines = (if (hasSelection) extractRange(content, range) else content)
                    .split('\n')
                    .filter { !it.startsWith(";") && it.isNotBlank() }
                    .fold(emptyList<String>()) { a, n ->
                        val last = a.lastOrNull() ?: ""
                        if (last.hasBalancedRoundBrackets().nok) {
                            val thisLine = last + n
                            val bb = thisLine.hasBalancedRoundBrackets()
                            if (bb.ok)
                                a.dropLast(1) + thisLine
                            else a + n
                        } else a + n
                    }

                for (line in lines) {
                    val res = evaluate(line, saveHistory = false, lsp = true)
                    if (res is err) {
                        val diag = Diagnostic()
                        diag.severity = DiagnosticSeverity.Error
                        diag.message = res.msg
                        diag.range = range

                        val diags = mutableListOf(diag)
                        val pdiag = PublishDiagnosticsParams(uri, diags)
                        client.publishDiagnostics(pdiag)
                        break
                    } else {
                        val diag = Diagnostic()
                        diag.severity = DiagnosticSeverity.Information
                        diag.message = res.toString()
                        diag.range = range

                        val diags = mutableListOf(diag)
                        val pdiag = PublishDiagnosticsParams(uri, diags)
                        client.publishDiagnostics(pdiag)
                    }
                }
            }
        }

        return completedFuture(null)
    }
}