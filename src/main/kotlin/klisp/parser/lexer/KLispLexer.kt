/**
 * Author: Paolo Gavocanov, based on the Scheme lexer by Matthew Might
 */

package klisp.parser.lexer

import klisp.Platform
import klisp.parser.lexer.lang.ANY_CHAR
import klisp.parser.lexer.lang.END
import klisp.parser.lexer.lang.RegularLanguage.Companion.notOneOf
import klisp.parser.lexer.lang.RegularLanguage.Companion.oneOf
import klisp.parser.lexer.tokens.*
import klisp.reversed
import klisp.singletonList
import klisp.took

/**
 * KLispLexer: A lexer for the programming language KLisp
 */
class KLispLexer : NBL() {
    // @formatter:off

    // Abbreviations:
    private val ch: RL = "\\".toRL CONCAT ANY_CHAR // character
    private val id: RL = (('A' thru 'Z') UNION ('a' thru 'z') UNION ('0' thru '9') UNION oneOf("-+/*_?%$#&^=!@<>:")).ONE_OR_MORE // identifier
    private val kw: RL = ":".toRL CONCAT id // keyword
    private val ws: RL = oneOf(" \r\t\n").ONE_OR_MORE // whitespace
    private val com: RL = ";".toRL CONCAT notOneOf("\r\n").ZERO_OR_MORE // single-line comment

    // Java numbers are crazy
    private val dot: RL = ".".toRL
    private val exp: RL = oneOf("eE")
    private val int: RL = "-".toRL.OPTIONAL CONCAT ('0' thru '9').ONE_OR_MORE
    private val dec: RL = (("-".toRL.OPTIONAL CONCAT dot.OPTIONAL) UNION (int CONCAT dot)) CONCAT int CONCAT (exp CONCAT int).OPTIONAL

    // States:
    override val MAIN: MajorLexerState = State()
    private val STRING: StatefulMajorLexerState<List<Char>> = State(emptyList())

    // Rules:
    init {
        // State switching rules
        MAIN switchesOn "\"" to { STRING apply emptyList() }

        // Regular tokens
        MAIN apply "(" apply { emit(OpenBraceToken("(")) }
        MAIN apply ")" apply { emit(CloseBraceToken(")")) }
        MAIN apply "true" apply { emit(BooleanToken(true)) }
        MAIN apply "false" apply { emit(BooleanToken(false)) }
        MAIN apply END apply { terminate() }
        MAIN apply ws apply { }
        MAIN apply com apply { }
        MAIN apply int over { cs -> emit(IntToken(cs.joinToString(""))) }
        MAIN apply dec over { cs -> emit(DecToken(cs.joinToString(""))) }
        MAIN apply kw over { cs -> emit(KeywordToken(cs.joinToString(""))) }
        MAIN apply id over { cs -> emit(SymbolToken(cs.joinToString(""))) }
        MAIN apply ch over { cs -> emit(CharToken(cs[1])) }

        // Strings
        STRING.update("\"") { s, _ -> emit(StringToken(s.reversed.joinToString(""))); MAIN }
        STRING.update("\\\"") { _, _ -> STRING apply '"'.singletonList }
        STRING.update("\\n") { _, _ -> STRING apply '\n'.singletonList }
        STRING.update("\\\\") { _, _ -> STRING apply '\\'.singletonList }
        STRING.update(ANY_CHAR) { ss, cs -> STRING apply cs.reversed + ss }
    }
    // @formatter:on

    companion object {
        // quick tests...
        @JvmStatic
        fun main(args: Array<String>) {
            val input = """ \na """
            val stream = LiveStream(input)
            val lexer = KLispLexer()
            val start = Platform.getTimeNanos()
            lexer.lex(stream)
            val took = took(start)
            println(lexer.output)
            println(lexer.output.toList)
            println(took)
        }
    }
}

