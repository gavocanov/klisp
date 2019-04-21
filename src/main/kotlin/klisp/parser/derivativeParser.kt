@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp.parser

import klisp.*
import klisp.parser.lexer.KLispLexer
import klisp.parser.lexer.LiveStream
import klisp.parser.lexer.tokens.*

fun derivativeParse(s: String): exp {
    val balance = s.hasBalancedRoundBrackets()
    if (balance.nok)
        throw IllegalArgumentException("unbalanced brackets <left: ${balance.left}, right: ${balance.right}>")
    val stream = LiveStream(s)
    val lexer = KLispLexer()
    lexer.lex(stream)
    return readFromTokens(lexer.output.toList)
}

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

fun parseTokenAtom(t: Token): atom = when (t) {
    is KeywordToken -> keyword(t.value)
    is StringToken -> string("\"${t.value}\"")
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

fun parseStringAtom(s: String): atom = when {
    s.startsWith(':') -> keyword(s)
    s.startsWith('"') && s.endsWith('"') -> string(s)
    s.startsWith('\\') && s.length == 2 -> char(s[1])
    else -> {
        val constructors = type
                .values()
                .filter(type::isNumber)
                .mapNotNull(type::constructor)

        var value: atom? = null
        for (f in constructors) {
            value = f(s) as atom
            if (value !== nil) break
        }

        if (value === nil || value === null) symbol(s)
        else value
    }
}
