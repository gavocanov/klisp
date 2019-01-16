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

expect class Queue<T>() : IQueue<T>

expect class FirstSet<T : Comparable<T>>() : Set<T> {
    constructor(from: T)
}

expect open class SortedSet<T : Comparable<T>>() : Set<T> {
    constructor(from: Collection<T>)
    constructor(from: T)
}

interface IFP {
    fun stabilized(): Boolean
    fun running(): Boolean
    fun changed(): Boolean
    fun generation(): Int
    fun master(): Any?

    fun stabilized(v: Boolean)
    fun running(v: Boolean)
    fun changed(v: Boolean)
    fun incGeneration()
    fun master(v: Any?)

    fun reset()
}

expect class FP() : IFP
