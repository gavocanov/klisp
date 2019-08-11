package klisp.lsp

import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint
import java.net.Socket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class SocketLauncher<T>(localService: Any, remoteInterface: Class<T>, socket: Socket) : Launcher<T> {
    private val launcher: Launcher<T> = Launcher.createLauncher(
        localService,
        remoteInterface,
        socket.getInputStream(),
        socket.getOutputStream()
    )

    override fun getRemoteProxy(): T = this.launcher.remoteProxy
    override fun getRemoteEndpoint(): RemoteEndpoint = this.launcher.remoteEndpoint
    override fun startListening(): CompletableFuture<Void> = CompletableFuture.runAsync(
        Runnable {
            this.launcher.startListening().get()
        },
        Executors.newFixedThreadPool(1, ThreadFactory {
            val th = Thread(it)
            th.name = "socket"
            th
        })
    )
}