package klisp

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.history.DefaultHistory
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import java.nio.file.Paths

actual fun getHistoryFileName(): String =
        Paths.get(System.getProperty("user.home"), HISTORY_FILE_NAME + "_jvm").toString()

actual fun loadHistory(fname: String): Boolean = try {
    HISTORY.load()
    println("loaded history from $fname")
    true
} catch (t: Throwable) {
    println("failed to load history from $fname: ${t.message ?: t::class.simpleName}")
    false
}

actual fun saveToHistory(l: String, fname: String, save: Boolean) {
    HISTORY.add(l)
    if (save)
        HISTORY.save()
}

val HISTORY = DefaultHistory()

val READER: LineReader = LineReaderBuilder.builder()
        .variable(LineReader.HISTORY_FILE, getHistoryFileName())
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

