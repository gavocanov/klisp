package klisp

expect object Platform {
    fun getenv(s: String): String?
    fun getProperty(s: String): String?
    fun console(): Boolean
    fun getHistoryFileName(): String
    fun loadHistory(fname: String): Boolean
    fun saveToHistory(l: String, fname: String, save: Boolean = true)
    fun readLine(prompt: String): String?
    fun exit(c: Int)
    fun getTimeNanos(): Long
    fun version(): String
    fun strFormat(d: Double): String
}