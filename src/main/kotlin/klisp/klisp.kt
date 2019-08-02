@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp

import klisp.parser.derivativeParse
import org.jline.builtins.Options
import kotlin.system.exitProcess

var PROFILE = false
var DEBUG = false
val VERSION_STR = "klisp-${Platform.platformId()} v$VERSION-$GIT_REVISION (git:$GIT_SHA), compiled on $BUILD_DATE"
const val HISTORY_FILE_NAME = ".kl_history"

fun main(args: Array<String>) {
    start(args)
}

private fun start(args: Array<String>) {
    LOGGER.info(VERSION_STR)

    val usage = arrayOf(
        "Usage:",
        "klisp - start a KLisp repl session",
        "  -h --help                show help"
    )

    val opts = Options.compile(usage).parse(args)
    if (opts.isSet("help")) {
        LOGGER.info(opts.usage())
        exitProcess(0)
    }

    repl()
}

fun repl() {
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

