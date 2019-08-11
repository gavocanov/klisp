package klisp.lsp

import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.services.WorkspaceService

class LSPWorkspace : WorkspaceService {
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) = Unit
    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) = Unit
}