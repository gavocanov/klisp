package klisp.lsp

import klisp.*
import org.eclipse.lsp4j.CompletionItemKind
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter

fun offset(content: String, position: Position) =
    offset(content, position.line, position.character)

/**
 * Convert from 0-based line and column to 0-based offset
 */
fun offset(content: String, line: Int, char: Int): Int {
    assert(!content.contains('\r'))

    val reader = content.reader()
    var offset = 0

    var lineOffset = 0
    while (lineOffset < line) {
        val nextChar = reader.read()
        check(nextChar != -1) { "reached end of file before reaching line $line" }
        if (nextChar.toChar() == '\n')
            lineOffset++
        offset++
    }

    var charOffset = 0
    while (charOffset < char) {
        val nextChar = reader.read()
        check(nextChar != -1) { "Reached end of file before reaching char $char" }
        charOffset++
        offset++
    }

    return offset
}

fun extractRange(content: String, range: Range) = content.substring(
    offset(content, range.start),
    offset(content, range.end)
)

fun String.toHoverCtx(pos: Position): String {
    val (_, e) = toRawPos(pos)
    val lineLeft = this
        .substring(0, e)
        .substringAfterLast('\n')
    val lineRight = this
        .substring(e)
        .substringBefore('\n')
    val line = lineLeft + lineRight
    val wordLeft = line
        .substring(0, pos.character)
        .reversed()
        .takeWhile(::isNotWordBoundary)
        .reversed()
    val wordRight = line
        .substring(pos.character)
        .takeWhile(::isNotWordBoundary)
    return (wordLeft + wordRight)
        .replace("(", "")
        .replace(")", "")
}

private fun isNotWordBoundary(c: Char) =
    c !in listOf(' ', '.', ')', '\n')

fun String.toRawPos(pos: Position): Pair<Int, Int> {
    val reader = BufferedReader(StringReader(this))
    val writer = StringWriter()
    // Skip unchanged lines
    var line = 0
    while (line < pos.line) {
        writer.write(reader.readLine() + '\n')
        line++
    }
    val start = writer.toString().length
    return start to (start + pos.character)
}

fun exp.toLspType(): CompletionItemKind = when (this) {
    is func -> CompletionItemKind.Function
    is symbol -> CompletionItemKind.Constant
    is keyword -> CompletionItemKind.Keyword
    is string -> CompletionItemKind.Text
    is number<*> -> CompletionItemKind.Constant
    else -> throw IllegalArgumentException("unknown type <$this>")
}

