package klisp

expect fun getHistoryFileName(): String
expect fun loadHistory(fname: String): Boolean
expect fun saveToHistory(l: String, fname: String, save: Boolean = true)
expect fun readLine(prompt: String): String?
expect fun exit(c: Int)
expect fun getTimeNanos(): Long
expect fun version(): String
