/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer.lang

/**
 * A regular expression that matches the end of the input.
 */
object END : RegularLanguage() {
    override infix fun derive(c: Char): NO_MATCH = NO_MATCH
    override fun deriveEND(): EMPTY = EMPTY
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "$$$"
}

/**
 * A regular expression that matches no strings at all.
 */
object NO_MATCH : RegularLanguage() {
    override infix fun derive(c: Char): NO_MATCH = this
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = true
    override val isEmptyString: Boolean = false
    override fun toString(): String = "∅"
}

/**
 * A regular expression that matches the empty string.
 */
object EMPTY : RegularLanguage() {
    override infix fun derive(c: Char): NO_MATCH = NO_MATCH
    override val acceptsEmptyString: Boolean = true
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = true
    override fun toString(): String = "ε"
}

/**
 * A regular expression that matches any character.
 */
object ANY_CHAR : RegularLanguage() {
    override infix fun derive(c: Char): EMPTY = EMPTY
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "."
}

