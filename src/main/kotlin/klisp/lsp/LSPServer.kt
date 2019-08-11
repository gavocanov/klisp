package klisp.lsp

import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.services.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class LSPServer : LanguageServer, LanguageClientAware {
    private val ws = LSPWorkspace()
    private val tds = LSPTextDocument()

    override fun getWorkspaceService(): WorkspaceService = ws

    override fun getTextDocumentService(): TextDocumentService = tds

    override fun connect(client: LanguageClient) {
        tds.connectClient(client)
    }

    override fun shutdown(): CompletableFuture<Any> = completedFuture(Unit)

    override fun exit() = Unit

    override fun initialize(params: InitializeParams?): CompletableFuture<InitializeResult> {
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