package klisp.parser.derivative.scheme

import klisp.Platform
import klisp.parser.derivative.AnyChar
import klisp.parser.derivative.BooleanToken
import klisp.parser.derivative.CharToken
import klisp.parser.derivative.END
import klisp.parser.derivative.IntToken
import klisp.parser.derivative.LiveStream
import klisp.parser.derivative.NonBlockingLexer
import klisp.parser.derivative.PunctToken
import klisp.parser.derivative.RegularLanguage
import klisp.parser.derivative.RegularLanguage.Companion.notOneOf
import klisp.parser.derivative.RegularLanguage.Companion.oneOf
import klisp.parser.derivative.StringToken
import klisp.parser.derivative.SymbolToken
import klisp.parser.derivative.Token
import klisp.reversed
import klisp.toListOf
import klisp.took

private typealias RL = RegularLanguage
private typealias NBL = NonBlockingLexer<Char, Token>

/**
 * KLLexer: A lexer for the programming language Scheme
 *
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 *
 */
class SXLexer : NBL() {
    // @formatter:off

    // Abbreviations:
    private val ch:  RL = "#\\".toRL() `~` AnyChar
    private val id:  RL = (('A' thru 'Z') `||` ('a' thru 'z') `||` ('0' thru '9') `||` oneOf("-+/*_?%$#&^=!@<>:")).`+`
    private val int: RL = "-".toRL().`?` `~` ('0' thru '9').`+`
    private val ws:  RL = oneOf(" \r\t\n").`+` // whitespace
    private val com: RL = ";".toRL() `~` notOneOf("\r\n").`*` // single-line comment

    // States:
    override val MAIN:        MajorLexerState                     = State()
    private  val BANGCOMMENT: StatefulMajorLexerState<Int>        = State(0)
    private  val STRING:      StatefulMajorLexerState<List<Char>> = State(emptyList())

    // Rules:
    init {
        // State switching rules
        MAIN switchesOn "#!" to { BANGCOMMENT apply 1 }
        MAIN switchesOn "\"" to { STRING apply emptyList() }

        // Regular tokens
        MAIN apply ",@" apply { emit(PunctToken(",@")) }
        MAIN apply ","  apply { emit(PunctToken(",")) }
        MAIN apply "`"  apply { emit(PunctToken("`")) }
        MAIN apply "'"  apply { emit(PunctToken("'")) }
        MAIN apply "#(" apply { emit(PunctToken("#(")) }
        MAIN apply "("  apply { emit(PunctToken("(")) }
        MAIN apply ")"  apply { emit(PunctToken(")")) }
        MAIN apply "["  apply { emit(PunctToken("[")) }
        MAIN apply "]"  apply { emit(PunctToken("]")) }
        MAIN apply "{"  apply { emit(PunctToken("{")) }
        MAIN apply "}"  apply { emit(PunctToken("}")) }
        MAIN apply "."  apply { emit(PunctToken(".")) }
        MAIN apply "#t" apply { emit(BooleanToken(true)) }
        MAIN apply "#f" apply { emit(BooleanToken(false)) }
        MAIN apply END  apply { terminate() }
        MAIN apply ws   apply { }
        MAIN apply com  apply { }
        MAIN apply ch   over  { cs -> emit(CharToken(cs[2])) }
        MAIN apply int  over  { cs -> emit(IntToken(cs.joinToString("").toInt())) }
        MAIN apply id   over  { cs -> emit(SymbolToken(cs.joinToString(""))) }

        // Strings
        STRING.update(s = "\"")        { s , _  -> emit(StringToken(s.reversed.joinToString(""))); MAIN }
        STRING.update(s = "\\\"")      { _ , _  -> STRING apply '"'.toListOf() }
        STRING.update(s = "\\n")       { _ , _  -> STRING apply '\n'.toListOf() }
        STRING.update(s = "\\\\")      { _ , _  -> STRING apply '\\'.toListOf() }
        STRING.update(regex = AnyChar) { ss, cs -> STRING apply cs.reversed + ss }

        // #! ... !# comments
        BANGCOMMENT.update(s = "#!")       { n, _ -> BANGCOMMENT apply n + 1 }
        BANGCOMMENT apply  AnyChar   apply { }
        BANGCOMMENT.update(s = "!#")       { n, _ -> when (n) {
                                                  1 -> MAIN
                                                  else -> BANGCOMMENT apply n - 1 }}
    }
    // @formatter:on
}

fun main(args: Array<String>) {
    val input = "(+ #\\1 (* #t #f) (+ \"test string\"))"
    val stream = LiveStream(input)
    val lexer = SXLexer()
    val start = Platform.getTimeNanos()
    lexer.lex(stream)
    val took = took(start)
    println(lexer.output)
    println(took)
}
