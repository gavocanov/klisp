/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer.lang

/**
 * A regular expression that matches a specific character.
 */
data class Character(private val ch: Char) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            if (c == ch) ε
            else `∅`

    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "'$ch'"
}

/**
 * A regular expression that matches a set of characters.
 */
data class CharSet(private val set: Set<Char>) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            if (set.contains(c)) ε
            else `∅`

    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean get() = set.isEmpty()
    override val isEmptyString: Boolean = false
    val not get() = NotCharSet(set)
}

/**
 * A regular expression that matches anything not in a set of characters.
 */
data class NotCharSet(private val set: Set<Char>) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            if (set.contains(c)) `∅`
            else ε

    override val acceptsEmptyString: Boolean = false
    // NOTE: If the set size is the same as the
    // number of unicode characters, it is the empty set:
    override val rejectsAll: Boolean get() = set.size == 100713
    override val isEmptyString: Boolean = false
    val not get() = CharSet(set)
}

/**
 * A regular expression that matches two regular expressions
 * in sequence.
 */
data class Catenation(val prefix: RegularLanguage,
                      val suffix: RegularLanguage) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            if (prefix.acceptsEmptyString)
                ((prefix derive c) `~` suffix) `||` (suffix derive c)
            else
                (prefix derive c) `~` suffix

    override val acceptsEmptyString: Boolean
        get() = prefix.acceptsEmptyString && suffix.acceptsEmptyString
    override val rejectsAll: Boolean
        get() = prefix.rejectsAll || suffix.rejectsAll
    override val isEmptyString: Boolean
        get() = prefix.isEmptyString && suffix.isEmptyString
}

/**
 * A regular expression that matches either of two regular expressions.
 */
data class Union(private val choice1: RegularLanguage,
                 private val choice2: RegularLanguage)
    : RegularLanguage() {

    override infix fun derive(c: Char): RegularLanguage =
            (choice1 derive c) `||` (choice2 derive c)

    override val acceptsEmptyString: Boolean
        get() = choice1.acceptsEmptyString || choice2.acceptsEmptyString
    override val rejectsAll: Boolean
        get() = choice1.rejectsAll && choice2.rejectsAll
    override val isEmptyString: Boolean
        get() = choice1.isEmptyString && choice2.isEmptyString
}

/**
 * A regular expression that matches zero or more repetitions of a regular expression.
 */
data class Star(private val regex: RegularLanguage) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            (regex derive c) `~` (regex.`*`)

    override val acceptsEmptyString: Boolean = true
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean
        get() = regex.isEmptyString || regex.isEmptyString
}

/**
 * A regular expression that matches exactly n repetitions a regular expression.
 */
data class Repetition(private val regex: RegularLanguage,
                      private val n: Int)
    : RegularLanguage() {

    override infix fun derive(c: Char): RegularLanguage =
            if (n <= 0) ε
            else (regex derive c) `~` (regex `^` (n - 1))

    override val acceptsEmptyString: Boolean
        get() = (n == 0) || ((n > 0) && regex.acceptsEmptyString)
    override val rejectsAll: Boolean
        get() = (n < 0) || regex.rejectsAll
    override val isEmptyString: Boolean
        get() = (n == 0) || ((n > 0) && regex.isEmptyString)
}
