package klisp.expected

import klisp.ExitException
import klisp.HISTORY_FILE_NAME
import klisp.LOGGER
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.utils.AttributedStyle
import java.nio.file.Paths
import java.text.NumberFormat
import java.util.ArrayDeque
import java.util.Stack

val HISTORY = DefaultHistory()

val READER: LineReader = LineReaderBuilder.builder()
        .variable(LineReader.HISTORY_FILE, Platform.getHistoryFileName())
        .history(HISTORY)
        .build()

private val WHITE: AttributedStyle = AttributedStyle().foreground(AttributedStyle.WHITE)
private val BLUE: AttributedStyle = AttributedStyle().foreground(AttributedStyle.BLUE)

actual object Platform {
    actual fun getHistoryFileName(): String =
            Paths.get(System.getProperty("user.home"), HISTORY_FILE_NAME + "_jvm").toString()

    actual fun loadHistory(fname: String): Boolean = try {
        HISTORY.load()
        LOGGER.info("loaded history from $fname")
        true
    } catch (t: Throwable) {
        LOGGER.warn("failed to load history from $fname: ${t.message ?: t::class.simpleName}")
        false
    }

    actual fun saveToHistory(l: String, fname: String, save: Boolean) {
        HISTORY.add(l)
        if (save)
            HISTORY.save()
    }

    actual fun strFormat(d: Double): String {
        val nf = NumberFormat.getInstance()
        nf.maximumFractionDigits = 3
        return nf.format(d)
    }

    actual fun readLine(prompt: String): String? = try {
        READER.readLine(prompt)
    } catch (ui: UserInterruptException) {
        throw ExitException("bye!!")
    } catch (eof: EndOfFileException) {
        throw ExitException("bye!!")
    } catch (t: Throwable) {
        throw t
    }

    actual fun platformId(): String = "jvm"
    actual fun exit(c: Int) = System.exit(c)
    actual fun getTimeNanos(): Long = System.nanoTime()

    actual fun getenv(s: String): String? = System.getenv(s)
    actual fun getProperty(s: String): String? = System.getProperty(s)
    actual fun console(): Boolean = System.console() !== null

    actual fun <T> copyArray(src: Array<T>, srcPos: Int, dest: Array<T?>, destPos: Int, length: Int) =
            System.arraycopy(src, srcPos, dest, destPos, length)
}

actual class Memoize<I, O> actual constructor(private val backingMap: MutableMap<I, O>,
                                              private val fn: (I) -> O) : (I) -> O {
    override fun invoke(i: I): O = backingMap.getOrPut(i) { fn(i) }
}

actual class Queue<T> actual constructor() : IQueue<T> {
    private val q = ArrayDeque<T>()
    override val isEmpty: Boolean
        get() = q.isEmpty()

    override fun dequeue(): T =
            q.pop()

    override fun plusAssign(items: Iterable<T>): Unit =
            q.plusAssign(items)

    override fun plusAssign(a: T): Unit =
            q.plusAssign(a)
}

actual class Stack<T> actual constructor() : IStack<T> {
    private val s: Stack<T> = Stack()

    override fun push(e: T) {
        s.push(e)
    }

    override fun pop(): T = s.pop()
    override fun isNotEmpty(): Boolean = s.isNotEmpty()
}

actual class FixedPoint actual constructor() : IFixedPoint {
    private var stabilized = ThreadLocal<Boolean>()
    private var running = ThreadLocal<Boolean>()
    private var changed = ThreadLocal<Boolean>()
    private var generation = ThreadLocal<Int>()
    private var master = ThreadLocal<Any?>()

    override fun stabilized(): Boolean = stabilized.get()
    override fun running(): Boolean = running.get()
    override fun changed(): Boolean = changed.get()
    override fun generation(): Int = generation.get()
    override fun master(): Any? = master.get()

    override fun stabilized(v: Boolean) = stabilized.set(v)
    override fun running(v: Boolean) = running.set(v)
    override fun changed(v: Boolean) = changed.set(v)
    override fun incGeneration() = generation.set(generation() + 1)
    override fun master(v: Any?) = master.set(v)

    init {
        reset()
    }

    override fun reset() {
        stabilized.set(false)
        running.set(false)
        changed.set(false)
        generation.set(0)
        master.set(null)
    }
}



