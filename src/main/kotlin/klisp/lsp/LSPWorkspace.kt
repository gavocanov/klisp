package klisp.lsp

import klisp.LOGGER
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.services.WorkspaceService

class LSPWorkspace : WorkspaceService {
    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams?) {
        LOGGER.lsp { "changeWatchedFiles: $params" }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams?) {
        LOGGER.lsp { "changeCfg: $params" }
    }
}