@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp

import klisp.lsp.LSPService
import klisp.parser.derivativeParse

var _PROFILE = false
var _DEBUG = false
val VERSION_STR = "klisp-${Platform.platformId()} v$VERSION-$GIT_REVISION (git:$GIT_SHA), compiled on $BUILD_DATE"
const val HISTORY_FILE_NAME = ".kl_history"

fun main() {
    start()
}

private fun start() {
    LOGGER.info(VERSION_STR + ", pid: " + ProcessHandle.current().pid())
    LSPService(11666)
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
            if (!line.isBlank()) {
                val res = evaluate(line, historyLoaded)
                if (res is err)
                    LOGGER.error(res.toString())
                else
                    LOGGER.info(res.toString())
            }
        } else
            Platform.exit(0)
    }
}

fun evaluate(code: String, saveHistory: Boolean): exp = try {
    var _start = if (_PROFILE) Platform.getTimeNanos() else 0
    val parsed = derivativeParse(code)
    val parseTook = took(_start)

    _start = if (_PROFILE) Platform.getTimeNanos() else 0
    val r = eval(parsed)
    val evalTook = took(_start)

    if (_PROFILE) {
        LOGGER.trace(":parse $parseTook")
        LOGGER.trace(":eval $evalTook")
    }

    Platform.saveToHistory(code, saveHistory)
    r
} catch (t: Throwable) {
    err(t.message ?: t::class.simpleName)
}

