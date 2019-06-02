@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp

import klisp.parser.derivativeParse
import org.apache.sshd.common.Factory
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.jline.builtins.Options
import java.io.InputStream
import java.io.OutputStream

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"
const val defaultPort = "11666"

fun main(args: Array<String>) {
    val usage = arrayOf(
        "klisp - start a KLisp session, repl or daemon (-d, ssh)",
        "Usage: klisp [-i ip] [-p port] [-d]",
        "  -i --ip=INTERFACE        listen interface (default=127.0.0.1), only when -d",
        "  -p --port=PORT           listen port (default=$defaultPort), only when -d",
        "  -d --daemon              start as an ssh server",
        "  -? --help                show help"
    )

    val opts = Options.compile(usage).parse(args)
    if (opts.isSet("help"))
        throw Options.HelpException(opts.usage());
    when {
        opts.isSet("daemon") -> ssh(opts)
        else -> repl()
    }
}

fun ssh(opts: Options) {
    logInfo()
    val server = sshServer(opts)
    server.start()
}

fun sshServer(opts: Options): SshServer = with(SshServer.setUpDefaultServer()) {
    setPasswordAuthenticator { _, _, _ -> true }
    keyPairProvider = SimpleGeneratorHostKeyProvider()
    shellFactory = KLShellFactory()
    if (opts.isSet("ip"))
        host = opts.get("ip")
    if (opts.isSet("port"))
        port = opts.getNumber("port")
    this
}

fun logInfo() = LOGGER
    .info("klisp-${Platform.platformId()} v$VERSION-$GIT_REVISION (git:$GIT_SHA), compiled on $BUILD_DATE")

fun repl() {
    logInfo()

    val historyFileName = Platform.getHistoryFileName()
    val historyLoaded = Platform.loadHistory(historyFileName)

    while (true) {
        val line = try {
            Platform.readLine("kl${Platform.platformId().first()} -> ")
        } catch (exit: ExitException) {
            LOGGER.info("bye!!")
            null
        } catch (t: Throwable) {
            LOGGER.error(t.message ?: t::class.simpleName)
            null
        }

        if (line !== null) {
            val res = try {
                var _start = if (PROFILE) Platform.getTimeNanos() else 0
                val parsed = derivativeParse(line)
                val parseTook = took(_start)

                _start = if (PROFILE) Platform.getTimeNanos() else 0
                val r = eval(parsed)
                val evalTook = took(_start)

                if (PROFILE) {
                    LOGGER.trace(":parse $parseTook")
                    LOGGER.trace(":eval $evalTook")
                }

                Platform.saveToHistory(line, historyLoaded)
                r
            } catch (t: Throwable) {
                err(t.message ?: t::class.simpleName)
            }
            if (res is err) LOGGER.error(res.toString())
            else LOGGER.info(res.toString())
        } else Platform.exit(0)
    }
}

class KLShellFactory : Factory<Command> {
    override fun create(): Command = Shell()

    private class Shell : Command, Runnable {
        override fun setExitCallback(callback: ExitCallback?) {
            TODO()
        }

        override fun setInputStream(`in`: InputStream?) {
            TODO()
        }

        override fun start(env: Environment?) {
            TODO()
        }

        override fun destroy() {
            TODO()
        }

        override fun setErrorStream(err: OutputStream?) {
            TODO()
        }

        override fun setOutputStream(out: OutputStream?) {
            TODO()
        }

        override fun run() {
            TODO()
        }
    }
}
