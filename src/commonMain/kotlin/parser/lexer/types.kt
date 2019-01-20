package klisp.parser.lexer

import klisp.parser.lexer.lang.RegularLanguage
import klisp.parser.lexer.tokens.Token

internal typealias RL = RegularLanguage
internal typealias NBL = NonBlockingLexer<Token>

/**
 * A token tag indicates the "type" of a token.
 *
 * For tokens generated by a lexer, it's appropriate to use a string.
 *
 * For example: "ID", "INT", "LPAREN", "SEMICOLON"
 *
 * There should be a finite number of token tags, so "ID(foo)" is a bad
 * token tag.
 *
 * During the parsing process, special tokens called parsing markers may
 * take control of this field.
 */
internal typealias TokenTag = Any

