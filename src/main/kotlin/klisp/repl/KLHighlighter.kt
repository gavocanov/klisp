package klisp.repl

import klisp.*
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedString.EMPTY
import org.jline.utils.AttributedString.join
import org.jline.utils.AttributedStyle

class KLHighlighter : Highlighter {
    companion object {
        private val SPACE = AttributedString(" ")
    }

    override fun highlight(reader: LineReader, buffer: String): AttributedString {
        val endSpace = buffer.endsWith(" ")

        val ass = buffer
            .split(" ")
            .map { w ->
                val word = w
                    .replace(")", "")
                    .replace("(", "")
                    .replace(":", "")

                if (word.isNotBlank()) {
                    join(
                        EMPTY,
                        AttributedString(w.substringBefore(word)),
                        doWord(word),
                        AttributedString(w.substringAfter(word))
                    )
                } else AttributedString(w)
            }

        return join(SPACE, if (endSpace) ass + SPACE else ass)
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