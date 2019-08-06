package klisp.repl

import klisp.*
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStyle

class KLHighlighter : Highlighter {
    companion object {
        private val PREFIXES = arrayOf("(" to AttributedString("("), ":" to AttributedString(":"))
        private val SUFFIXES = arrayOf(")" to AttributedString(")"))
        private val SPACE = AttributedString(" ")
    }

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val endSpace = buffer.endsWith(" ")
        val ass = buffer
            .split(" ")
            .map { w ->
                val p = PREFIXES.mapNotNull { (p, ap) ->
                    if (w.startsWith(p) && w.length > 1) {
                        AttributedString.join(
                            AttributedString.EMPTY,
                            ap,
                            doWord(w.substringAfter(p))
                        )
                    } else null
                }

                val s = SUFFIXES.mapNotNull { (s, ass) ->
                    if (w.endsWith(s) && w.length > 1) {
                        AttributedString.join(
                            AttributedString.EMPTY,
                            doWord(w.substringBefore(s)),
                            ass
                        )
                    } else null
                }

                val r = p + s
                if (r.isNotEmpty()) r
                else listOf(doWord(w))
            }.flatten()
        val joined = AttributedString.join(SPACE, ass)
        return if (endSpace) AttributedString.join(AttributedString.EMPTY, joined, SPACE)
        else joined
    }

    private fun doWord(word: String): AttributedString {
        if (specialForm.isSpecial(word))
            return AttributedString(word, AttributedStyle.BOLD)

        val i = stdEnv
            .filter { (v, _) -> v.value == word }
            .toList()
            .firstOrNull()

        if (i === null)
            return AttributedString(word)

        return when (i.second) {
            is func -> AttributedString(word, AttributedStyle().foreground(AttributedStyle.GREEN))
            is number<*> -> AttributedString(word, AttributedStyle().foreground(AttributedStyle.BLUE))
            is string, is char -> AttributedString(word, AttributedStyle().foreground(AttributedStyle.MAGENTA))
            is symbol -> AttributedString(word, AttributedStyle().foreground(AttributedStyle.CYAN))
            is keyword -> AttributedString(word, AttributedStyle.BOLD)
            else -> AttributedString(word)
        }
    }
}