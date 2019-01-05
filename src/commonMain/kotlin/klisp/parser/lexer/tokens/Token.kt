/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer.tokens

import klisp.parser.lexer.TokenTag

/**
 * Parsers consume live streams of tokens (or objects that can be implicitly converted into them).
 *
 * A token is an individual lexeme.
 *
 * (In terms of context-free grammars, tokens are the terminals.)
 */
abstract class Token : Comparable<Token> {
    /**
     * @return true if this token represents a parsing marker.
     *
     * Parsing markers are special tokens that do not appear in input
     * strings; they only appear in parse strings to indicate the
     * structure of the parse.
     */
    abstract val isParsingMarker: Boolean

    /**
     * The class of this token.
     */
    protected open val clazz: String
        get() = this::class.toString()

    /**
     * The tag of this token.
     *
     * A token's tag indicates to which lexical class it belongs.
     *
     * Tokens consumed as input should have strings for their tags.
     *
     * Examples of good tags would be, "Identifier", "Integer", "String", ";", "(", ")".
     *
     * Parsing markers will have special tags.
     */
    abstract val tag: TokenTag

    protected abstract fun localCompare(other: Token): Int

    override fun compareTo(other: Token): Int {
        val c1 = clazz.compareTo(other.clazz)
        if (c1 != 0)
            return c1
        return localCompare(other)
    }
}