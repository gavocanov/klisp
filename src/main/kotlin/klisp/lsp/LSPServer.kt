package klisp.lsp

import klisp.LOGGER
import klisp.lsp
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class LSPServer : LanguageServer, LanguageClientAware {
    private val ws = LSPWorkspace()
    private val tds = LSPTextDocument()

    override fun getWorkspaceService(): WorkspaceService =
        ws.also { LOGGER.lsp { "getWorkspaceService" } }

    override fun getTextDocumentService(): TextDocumentService =
        tds.also { LOGGER.lsp { "getTextDocService" } }

    override fun connect(client: LanguageClient) {
        LOGGER.lsp { "lsp client connected" }
        tds.connectClient(client)
    }

    override fun shutdown(): CompletableFuture<Any> {
        LOGGER.lsp { "shutdown" }
        return completedFuture(Unit)
    }

    override fun exit() {
        LOGGER.lsp { "exit" }
    }

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
        LOGGER.lsp { "init: $params" }

        val capabilities = ServerCapabilities()

        capabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental)
        capabilities.workspace = WorkspaceServerCapabilities()
        capabilities.workspace.workspaceFolders = WorkspaceFoldersOptions()
        capabilities.workspace.workspaceFolders.supported = true
        capabilities.workspace.workspaceFolders.changeNotifications = Either.forRight(true)
        capabilities.completionProvider = CompletionOptions(false, listOf("."))
        capabilities.hoverProvider = true
        capabilities.documentSymbolProvider = true

        capabilities.definitionProvider = false
        capabilities.workspaceSymbolProvider = false
        capabilities.referencesProvider = false
        capabilities.codeActionProvider = Either.forLeft(false)

        return completedFuture(InitializeResult(capabilities))
    }

    fun closeClient() {
        tds.disconnectClient()
    }
}