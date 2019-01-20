/**
 * Author: Paolo Gavocanov, based on the Scheme lexer by Matthew Might
 */

package klisp.parser.lexer

import klisp.expected.Platform
import klisp.parser.lexer.lang.AnyChar
import klisp.parser.lexer.lang.END
import klisp.parser.lexer.lang.RegularLanguage.Companion.notOneOf
import klisp.parser.lexer.lang.RegularLanguage.Companion.oneOf
import klisp.parser.lexer.tokens.BooleanToken
import klisp.parser.lexer.tokens.CloseBraceToken
import klisp.parser.lexer.tokens.IntToken
import klisp.parser.lexer.tokens.OpenBraceToken
import klisp.parser.lexer.tokens.StringToken
import klisp.parser.lexer.tokens.SymbolToken
import klisp.reversed
import klisp.singletonList
import klisp.took
import kotlin.jvm.JvmStatic

/**
 * KLispLexer: A lexer for the programming language KLisp
 */
class KLispLexer : NBL() {
    // @formatter:off

    // Abbreviations:
    private val id:  RL = (('A' thru 'Z') `||` ('a' thru 'z') `||` ('0' thru '9') `||` oneOf("-+/*_?%$#&^=!@<>:")).`+`
    private val int: RL = "-".toRL.`?` `~` ('0' thru '9').`+`
    private val ws:  RL = oneOf (" \r\t\n").`+` // whitespace
    private val com: RL = ";".toRL `~` notOneOf ("\r\n").`*` // single-line comment

    // States:
    override val MAIN:        MajorLexerState                     = State()
    private  val STRING:      StatefulMajorLexerState<List<Char>> = State(emptyList())

    // Rules:
    init {
        // State switching rules
        MAIN switchesOn "\"" to { STRING apply emptyList() }

        // Regular tokens
        MAIN apply "("     apply { emit(OpenBraceToken("(")) }
        MAIN apply ")"     apply { emit(CloseBraceToken(")")) }
        MAIN apply "true"  apply { emit(BooleanToken(true)) }
        MAIN apply "false" apply { emit(BooleanToken(false)) }
        MAIN apply END     apply { terminate() }
        MAIN apply ws      apply { }
        MAIN apply com     apply { }
        MAIN apply int     over  { cs -> emit(IntToken(cs.joinToString("").toInt())) }
        MAIN apply id      over  { cs -> emit(SymbolToken(cs.joinToString(""))) }

        // Strings
        STRING.update("\"")        {  s ,  _  -> emit(StringToken(s.reversed.joinToString(""))); MAIN }
        STRING.update("\\\"")      {  _ ,  _  -> STRING apply '"'.singletonList }
        STRING.update("\\n")       {  _ ,  _  -> STRING apply '\n'.singletonList }
        STRING.update("\\\\")      {  _ ,  _  -> STRING apply '\\'.singletonList }
        STRING.update(AnyChar)        { ss , cs  -> STRING apply cs.reversed + ss }
    }
    // @formatter:on

    companion object {
        // quick tests...
        @JvmStatic
        fun main(args: Array<String>) {
            val input = "(+ 1  true \"aaa bbb\" if then else)"
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

