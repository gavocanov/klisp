package klisp.lsp

import klisp.LOGGER
import org.eclipse.lsp4j.services.LanguageClient
import java.net.ServerSocket

class LSPService(private val port: Int) {
    init {
        Thread(Runnable { lsp() }, "lsp").start()
    }

    private fun lsp() {
        val server = LSPServer()
        LOGGER.info("started LSP server on port: $port")
        ServerSocket(port).use { ss ->
            while (true) {
                val socket = ss.accept()
                val launcher = SocketLauncher(server, LanguageClient::class.java, socket)
                server.connect(launcher.remoteProxy)
                launcher.startListening().thenRun {
                    server.closeClient()
                }
            }
        }
    }
}