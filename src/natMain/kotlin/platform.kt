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

/**
 * Not supported for now in native, throws "mutation attempt of frozen kotlin.collections.HashMap"
 */
actual class Memoize<I, O> actual constructor(private val backingMap: MutableMap<I, O>,
                                              private val fn: (I) -> O) : (I) -> O {
    override fun invoke(p1: I): O = fn(p1)
}

actual class SortedSet<T : Comparable<T>> : Set<T> {
    private val lst: List<T>

    actual constructor(from: T) {
        lst = listOf(from).sorted()
    }

    actual constructor(from: Collection<T>) {
        lst = from.sorted()
    }

    actual constructor() {
        lst = emptyList()
    }

    override val size: Int get() = lst.size
    override fun contains(element: T): Boolean = lst.binarySearch(element) != -1
    override fun containsAll(elements: Collection<T>): Boolean = elements.all(::contains)
    override fun isEmpty(): Boolean = lst.isEmpty()
    override fun iterator(): Iterator<T> = lst.iterator()
}

actual class Queue<T> actual constructor() : IQueue<T> {
    private val q: ArrayList<T> = arrayListOf()

    override val isEmpty: Boolean get() = q.isEmpty()

    override fun dequeue(): T = if (!isEmpty)
        q.removeAt(0)
    else
        throw NoSuchElementException("queue is empty")

    override operator fun plusAssign(items: Iterable<T>) {
        q.addAll(items)
    }

    override operator fun plusAssign(a: T) {
        q.add(a)
    }

    override fun toString(): String = q.toString()
}

