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
import platform.posix.getenv

actual fun getHistoryFileName(): String {
    val home = getenv("HOME")?.toKString() ?: throw IllegalStateException("failed to determine user home")
    return "$home/$HISTORY_FILE_NAME"
}

actual fun loadHistory(fname: String): Boolean {
    val file = fopen(fname, "a+")
    fclose(file)
    linenoiseHistorySetMaxLen(64_000)
    linenoiseSetMultiLine(1)
    val loaded = linenoiseHistoryLoad(fname) == 0
    if (!loaded)
        println("failed to load history file $fname")
    return loaded
}

actual fun saveToHistory(l: String, fname: String, save: Boolean) {
    linenoiseHistoryAdd(l)
    if (save) linenoiseHistorySave(fname)
}

actual fun readLine(prompt: String): String? =
        linenoise(prompt)?.toKString()

actual fun exit(c: Int) = platform.posix.exit(c)

actual fun getTimeNanos(): Long = kotlin.system.getTimeNanos()

actual fun version(): String = "nat"
