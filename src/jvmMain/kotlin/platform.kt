package klisp

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.utils.AttributedStyle
import java.nio.file.Paths
import java.text.NumberFormat
import java.util.ArrayDeque
import java.util.SortedSet
import java.util.TreeSet

val HISTORY = DefaultHistory()

val READER: LineReader = LineReaderBuilder.builder()
        .variable(LineReader.HISTORY_FILE, Platform.getHistoryFileName())
        .history(HISTORY)
/*        .highlighter { _, buffer ->
            val l = tokenize(buffer)
            l.fold(AttributedStringBuilder()) { builder, t ->
                val s = stdEnv[symbol(t)]
                if (s !== null)
                    builder
                            .append("$t ", BLUE)
                else {
                    when {
                        t.startsWith(':') -> builder.append("$t ", BLUE)
                        t == "(" -> builder.append(t, WHITE)
                        t == ")" -> builder.append(t, WHITE)
                        t == "{" -> builder.append(t, WHITE)
                        t == "}" -> builder.append(t, WHITE)
                        t == "[" -> builder.append(t, WHITE)
                        t == "]" -> builder.append(t, WHITE)
                        else -> builder.append("$t ", AttributedStyle.DEFAULT)
                    }
                }
            }.toAttributedString()
        }*/
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

    actual fun version(): String = "jvm"
    actual fun exit(c: Int) = System.exit(c)
    actual fun getTimeNanos(): Long = System.nanoTime()

    actual fun getenv(s: String): String? = System.getenv(s)
    actual fun getProperty(s: String): String? = System.getProperty(s)
    actual fun console(): Boolean = System.console() !== null
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

actual class SortedSet<T : Comparable<T>>(private val set: TreeSet<T> = TreeSet()) : SortedSet<T> by set, Set<T> {
    actual constructor() : this(TreeSet())
    actual constructor(from: Collection<T>) : this(TreeSet(from.sorted()))
    actual constructor(from: T) : this(TreeSet(listOf(from).sorted()))
}
