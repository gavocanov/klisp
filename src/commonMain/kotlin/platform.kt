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

expect class Memoize<I, O>(backingMap: MutableMap<I, O>, fn: (I) -> O) : (I) -> O

interface IQueue<T> {
    val isEmpty: Boolean
    fun dequeue(): T

    operator fun plusAssign(items: Iterable<T>)

    operator fun plusAssign(a: T)
}

expect class Queue<T>(): IQueue<T>

expect class SortedSet<T : Comparable<T>>(from: Collection<T>) : Set<T> {
    constructor(from: T)
    constructor()
}
