package klisp

import org.jline.reader.*
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle
import java.nio.file.Paths
import java.text.NumberFormat
import java.util.*
import kotlin.system.exitProcess

val HISTORY = DefaultHistory()

val TERMINAL: Terminal = TerminalBuilder.builder()
    .system(true)
    .jansi(true)
    .build()

val READER: LineReader = LineReaderBuilder.builder()
    .variable(LineReader.HISTORY_FILE, Platform.getHistoryFileName())
    .history(HISTORY)
    .terminal(TERMINAL)
    .completer(KlispCompleter())
    .highlighter(KlispHighlighter())
    .build()

class KlispHighlighter : Highlighter {
    override fun highlight(reader: LineReader?, buffer: String?): AttributedString {
        if (buffer in stdEnv.keys.map { it.value })
            return AttributedString(buffer, AttributedStyle.BOLD)
        return AttributedString(buffer)
    }
}

class KlispCompleter : Completer {
    override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
        candidates.addAll(
            stdEnv.map { (v, t) ->
                Candidate(
                    v.value,
                    v.value,
                    t::class.java.simpleName,
                    null,
                    null,
                    null,
                    true
                )
            }
        )
    }
}

object Platform {
    fun getHistoryFileName(): String =
        Paths.get(System.getProperty("user.home"), HISTORY_FILE_NAME + "_jvm").toString()

    fun loadHistory(fname: String): Boolean = try {
        HISTORY.load()
        LOGGER.trace("loaded history from $fname")
        true
    } catch (t: Throwable) {
        LOGGER.trace("failed to load history from $fname: ${t.message ?: t::class.simpleName}")
        false
    }

    fun saveToHistory(l: String, save: Boolean) {
        HISTORY.add(l)
        if (save)
            HISTORY.save()
    }

    fun strFormat(d: Double): String {
        val nf = NumberFormat.getInstance()
        nf.maximumFractionDigits = 3
        return nf.format(d)
    }

    fun readLine(prompt: String): String? = try {
        READER.readLine(prompt)
    } catch (ui: UserInterruptException) {
        throw ExitException("bye!!")
    } catch (eof: EndOfFileException) {
        throw ExitException("bye!!")
    } catch (t: Throwable) {
        throw t
    }

    fun platformId(): String = "jvm"
    fun exit(c: Int): Nothing = exitProcess(c)
    fun getTimeNanos(): Long = System.nanoTime()

    fun getenv(s: String): String? = System.getenv(s)
    fun getProperty(s: String): String? = System.getProperty(s)
    fun console(): Boolean = System.console() !== null
}

class Memoize<I, O> constructor(private val backingMap: MutableMap<I, O>,
                                private val fn: (I) -> O) : (I) -> O {
    override fun invoke(i: I): O = backingMap.getOrPut(i) { fn(i) }
}

class Queue<T> {
    private val q = ArrayDeque<T>()

    val isEmpty: Boolean get() = q.isEmpty()

    fun dequeue(): T = q.pop()
    operator fun plusAssign(items: Iterable<T>): Unit = q.plusAssign(items)
    operator fun plusAssign(a: T): Unit = q.plusAssign(a)
}
