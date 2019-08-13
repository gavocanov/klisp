package klisp.lsp

import com.google.gson.Gson
import com.google.gson.JsonElement
import klisp.LOGGER
import klisp.err
import klisp.evaluate
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageClientAware
import org.eclipse.lsp4j.services.WorkspaceService
import java.util.concurrent.CompletableFuture

class LSPWorkspace : WorkspaceService, LanguageClientAware {
    companion object {
        const val EVAL = "eval"
        private val gson = Gson()
    }

    private lateinit var languageClient: LanguageClient

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) = Unit
    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) = Unit

    override fun connect(client: LanguageClient) {
        languageClient = client
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any> {
        val args = params.arguments
        when (params.command) {
            EVAL -> {
                val content = gson.fromJson(args[0] as JsonElement, String::class.java)
//                val range = gson.fromJson(args[1] as JsonElement, Range::class.java)

                val lines = content
                    .split('\n')
                    .filter { !it.startsWith(";") && it.isNotBlank() }

                for (line in lines) {
                    val res = evaluate(line, false)
                    if (res is err) {
                        LOGGER.error("\nerror evaluating '$line':\n$res")
                        break
                    } else LOGGER.info("\n$res")
                }
            }
        }

        return CompletableFuture.completedFuture(null)
    }
}