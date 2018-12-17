package klisp

import kotlinx.cinterop.toKString
import linenoise.linenoise
import linenoise.linenoiseHistoryAdd
import linenoise.linenoiseHistoryLoad
import linenoise.linenoiseHistorySave
import linenoise.linenoiseHistorySetMaxLen
import linenoise.linenoiseSetMultiLine
import platform.posix.fclose
import platform.posix.fopen

actual object Platform {
    actual fun getHistoryFileName(): String {
        val home = getenv("HOME")
                ?: throw IllegalStateException("failed to determine user home")
        return "$home/$HISTORY_FILE_NAME"
    }

    actual fun loadHistory(fname: String): Boolean {
        val file = fopen(fname, "a+")
        fclose(file)
        linenoiseHistorySetMaxLen(64_000)
        linenoiseSetMultiLine(1)
        val loaded = linenoiseHistoryLoad(fname) == 0
        if (!loaded)
            LOGGER.warn("failed to load history file $fname")
        return loaded
    }

    actual fun saveToHistory(l: String, fname: String, save: Boolean) {
        linenoiseHistoryAdd(l)
        if (save) linenoiseHistorySave(fname)
    }

    actual fun strFormat(d: Double): String = d.toString()

    actual fun readLine(prompt: String): String? =
            linenoise(prompt)?.toKString()

    actual fun exit(c: Int) = platform.posix.exit(c)

    actual fun getTimeNanos(): Long = kotlin.system.getTimeNanos()

    actual fun version(): String = "nat"

    actual fun getenv(s: String): String? = platform.posix.getenv(s)?.toKString()
    actual fun getProperty(s: String): String? = getenv(s)
    actual fun console(): Boolean = true
}

