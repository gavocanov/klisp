package klisp

actual fun getHistoryFileName(): String = ""
actual fun loadHistory(fname: String): Boolean = false
actual fun saveToHistory(l: String, fname: String, save: Boolean) = Unit
actual fun exit(c: Int) = System.exit(c)
actual fun getTimeNanos(): Long = System.nanoTime()
actual fun readLine(prompt: String): String? {
    print(prompt); return readLine()
}

