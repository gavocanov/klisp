package klisp

import klisp.expected.Platform
import klisp.parser.derivativeParse

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    LOGGER.info("$MAVEN_GROUP-${Platform.platformId()} v$VERSION-$GIT_REVISION (git:$GIT_SHA), compiled on $BUILD_DATE")

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

                Platform.saveToHistory(line, historyFileName, historyLoaded)
                r
            } catch (t: Throwable) {
                err(t.message ?: t::class.simpleName)
            }
            if (res is err) LOGGER.error(res.toString())
            else LOGGER.info(res.toString())
        } else Platform.exit(0)
    }
}
