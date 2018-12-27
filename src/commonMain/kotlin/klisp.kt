package klisp

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    LOGGER.info("**klisp ${Platform.version()}**")

    val historyFileName = Platform.getHistoryFileName()
    val historyLoaded = Platform.loadHistory(historyFileName)

    while (true) {
        val line = try {
            Platform.readLine("kl${Platform.version().first()} -> ")
        } catch (exit: ExitException) {
            LOGGER.info("bye!!")
            null
        } catch (t: Throwable) {
            LOGGER.error(t.message ?: t::class.simpleName)
            null
        }

        if (line !== null) {
            val res = try {
                val _start = if (PROFILE) Platform.getTimeNanos() else 0
                val r = eval(parse(line))
                if (PROFILE) LOGGER.trace(":eval/parse ${took(_start)}")
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
