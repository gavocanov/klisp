package klisp.expected

import klisp.HISTORY_FILE_NAME
import klisp.LOGGER
import kotlinx.cinterop.toKString
import linenoise.linenoise
import linenoise.linenoiseHistoryAdd
import linenoise.linenoiseHistoryLoad
import linenoise.linenoiseHistorySave
import linenoise.linenoiseHistorySetMaxLen
import linenoise.linenoiseSetMultiLine
import platform.posix.fclose
import platform.posix.fopen
import kotlin.native.concurrent.ThreadLocal

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

    actual fun platformId(): String = "nat"

    actual fun getenv(s: String): String? = platform.posix.getenv(s)?.toKString()
    actual fun getProperty(s: String): String? = getenv(s)
    actual fun console(): Boolean = true

    actual fun <T> copyArray(src: Array<T>, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int) =
            (0 until length).forEach {
                dest[destPos + it] = src[srcPos + it]
            }
}

@ThreadLocal
actual class Memoize<I, O> actual constructor(private val backingMap: MutableMap<I, O>,
                                              private val fn: (I) -> O) : (I) -> O {
    override fun invoke(p1: I): O = fn(p1)
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

actual class Stack<T> actual constructor() : IStack<T> {
    private val s: ArrayList<T> = arrayListOf()

    override fun push(e: T) {
        s.add(0, e)
    }

    override fun pop(): T {
        val r = s[s.size - 1]
        s.removeAt(s.size - 1)
        return r
    }

    override fun isNotEmpty(): Boolean = s.isNotEmpty()
}

@ThreadLocal
actual class FixedPoint actual constructor() : IFixedPoint {
    private var stabilized = false
    private var running = false
    private var changed = false
    private var generation = 0
    private var master: Any? = null

    override fun stabilized(): Boolean = stabilized
    override fun running(): Boolean = running
    override fun changed(): Boolean = changed
    override fun generation(): Int = generation
    override fun master(): Any? = master

    override fun stabilized(v: Boolean) {
        stabilized = v
    }

    override fun running(v: Boolean) {
        running = v
    }

    override fun changed(v: Boolean) {
        changed = v
    }

    override fun incGeneration() {
        generation += 1
    }

    override fun master(v: Any?) {
        master = v
    }

    override fun reset() {
        stabilized = false
        running = false
        changed = false
        generation = 0
        master = null
    }
}
