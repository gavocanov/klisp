package klisp.lsp

import klisp.LOGGER
import org.eclipse.lsp4j.services.LanguageClient
import java.net.ServerSocket

class LSPService(val port: Int) {
    init {
        try {
            Thread(Runnable { lsp() }, "lsp").start()
        } catch (e: Exception) {
            LOGGER.error("error in lsp init")
        }
    }

    private fun lsp() {
        val server = LSPServer()
        try {
            ServerSocket(port, 2).use { ss ->
                LOGGER.info("lsp server started at: ${ss.inetAddress}:${ss.localPort}")
                while (true) {
                    try {
                        val socket = ss.accept()
                        val launcher = SocketLauncher(server, LanguageClient::class.java, socket)
                        server.connect(launcher.remoteProxy)
                        launcher.startListening().thenRun { server.closeClient() }
                    } catch (t: Throwable) {
                        LOGGER.warn(t.message)
                    }
                }
            }
        } catch (e: Exception) {
            LOGGER.error("error in lsp init")
        }
    }
}