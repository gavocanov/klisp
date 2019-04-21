/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer.lang

/**
 * A regular expression that matches the end of the input.
 */
object END : RegularLanguage() {
    override infix fun derive(c: Char): `∅` = `∅`
    override fun deriveEND(): ε = ε
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "$$$"
}

/**
 * A regular expression that matches no strings at all.
 */
@Suppress("NonAsciiCharacters", "unused")
object `∅` : RegularLanguage() {
    override infix fun derive(c: Char): `∅` = this
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = true
    override val isEmptyString: Boolean = false
    override fun toString(): String = "∅"
}

/**
 * A regular expression that matches the empty string.
 */
@Suppress("NonAsciiCharacters")
object ε : RegularLanguage() {
    override infix fun derive(c: Char): `∅` = `∅`
    override val acceptsEmptyString: Boolean = true
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = true
    override fun toString(): String = "ε"
}

/**
 * A regular expression that matches any character.
 */
object AnyChar : RegularLanguage() {
    override infix fun derive(c: Char): ε = ε
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "."
}

