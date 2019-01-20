package klisp.parser

import klisp._list
import klisp.atom
import klisp.bool
import klisp.char
import klisp.exp
import klisp.head
import klisp.keyword
import klisp.nil
import klisp.nok
import klisp.parser.derivative.BooleanToken
import klisp.parser.derivative.CharToken
import klisp.parser.derivative.CloseBraceToken
import klisp.parser.derivative.KeywordToken
import klisp.parser.derivative.LiveStream
import klisp.parser.derivative.OpenBraceToken
import klisp.parser.derivative.StringToken
import klisp.parser.derivative.SymbolToken
import klisp.parser.derivative.Token
import klisp.parser.derivative.klisp.KLLexer
import klisp.string
import klisp.symbol
import klisp.type
import klisp.unit

@ExperimentalUnsignedTypes
fun derivativeParse(s: String): exp {
    val balance = s.hasBalancedRoundBrackets()
    if (balance.nok)
        throw IllegalArgumentException("unbalanced brackets <left: ${balance.left}, right: ${balance.right}>")
    val stream = LiveStream(s)
    val lexer = KLLexer()
    lexer.lex(stream)
    return readFromTokens(lexer.output.toList.toMutableList())
}

@ExperimentalUnsignedTypes
fun readFromTokens(tokens: MutableList<Token>): exp {
    if (tokens.isEmpty()) return unit
    val token = tokens.head
    tokens.removeAt(0)
    return when (token) {
        is OpenBraceToken -> {
            val list = mutableListOf<exp>()
            try {
                while (tokens[0] !is CloseBraceToken)
                    list.add(readFromTokens(tokens))
                tokens.removeAt(0)
            } catch (t: Throwable) {
                throw IllegalArgumentException("parsing failed")
            }
            _list(list)
        }
        is CloseBraceToken -> throw IllegalArgumentException("unexpected )")
        else -> parseTokenAtom(token)
    }
}

@ExperimentalUnsignedTypes
fun parseTokenAtom(t: Token): atom = when (t) {
    is KeywordToken -> keyword(t.value)
    is StringToken -> string(t.value)
    is CharToken -> char(t.value)
    is SymbolToken -> symbol(t.value)
    is BooleanToken -> bool(t.value)
    else -> {
        val constructors = type
                .values()
                .filter(type::isNumber)
                .mapNotNull(type::constructor)

        var value: atom? = null
        for (f in constructors) {
            value = f(t.toString()) as atom
            if (value !== nil) break
        }

        if (value === nil || value === null) symbol(t.toString())
        else value
    }
}
