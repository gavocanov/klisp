/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 *
 * This file provides a non-blocking lexing toolkit based on
 * derivatives.
 *
 * A lexer is usually the first phase of a compiler, turning a stream of
 * characters into a stream of tokens.
 *
 * A non-blocker lexer consumes one character at a time, suspending
 * itself and yielding control when it does not have enough input to
 * continue.
 *
 * Since it doesn't block, this lexer can be used in
 * performance-sensitive situations, such as network protocol
 * negotiation.
 *
 * A lexer is a state machine in which every state contains rules, and
 * each rule contains both a regular expression and an action.
 *
 * A lexer executes by looking for the longest matching rule in the
 * current state and then firing its associated action.
 *
 * Each action can emit zero or more tokens downstream and indicate the
 * next lexing state.
 */
@file:Suppress("unused")

package klisp.parser.derivative

import klisp.Memoize

/**
 * A regular language is the set of strings constructed from union,
 * concatenation and repetition.
 */
abstract class RegularLanguage {
    companion object {
        /**
         * Return a language over the characters in the provided string.
         */
        infix fun oneOf(s: String): CharSet = CharSet(s.toSet())

        infix fun notOneOf(s: String): NotCharSet = NotCharSet(s.toSet())
    }

    /**
     * @return the derivative of this regular expression.
     */
    abstract infix fun derive(c: Char): RegularLanguage

    /**
     * @return the derivative of this regular expression with respect
     * to the end of the input.
     */
    open fun deriveEND(): RegularLanguage = EmptySet

    /**
     * @return true iff the regular expression accepts the empty string.
     */
    abstract val acceptsEmptyString: Boolean

    /**
     * @return true if the regular expression accepts no strings at all.
     */
    abstract val rejectsAll: Boolean

    /**
     * @return true iff this regular expression accepts only the empty string.
     */
    abstract val isEmptyString: Boolean

    /**
     * @return true if this regular expression is definitely a subset of
     * another regular expression.
     */
    private infix fun mustBeSubsumedBy(re2: RegularLanguage): Boolean {
        val (c1, c2) = this to re2
        return when {
            c1 is Character && c2 is Character -> c1 == c2
            c1 is Character && c2 is AnyChar -> true
            else -> false
        }
    }

    /**
     * Kleene star; zero or more repetitions
     * @return zero or more repetitions of this regular expression.
     */
    val `*`: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> Epsilon
            else -> Star(this)
        }

    /**
     * Exactly n repetitions
     */
    infix fun `^`(n: Int): RegularLanguage = when {
        n < 0 -> EmptySet
        n == 0 -> Epsilon
        n == 1 -> this
        isEmptyString -> Epsilon
        rejectsAll -> EmptySet
        else -> Repetition(this, n)
    }

    /**
     * @return one or more repetitions of this regular expression.
     */
    val `+`: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> EmptySet
            else -> this `~` Star(this)
        }

    /**
     * @return the option of this regular expression.
     */
    val `?`: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> Epsilon
            else -> Epsilon `||` this
        }

    /**
     * Concatenation
     * @return the smart concatenation of this and another regular expression.
     */
    infix fun `~`(suffix: RegularLanguage): RegularLanguage = when {
        isEmptyString -> suffix
        suffix.isEmptyString -> this
        rejectsAll -> EmptySet
        suffix.rejectsAll -> EmptySet
        else -> Catenation(this, suffix)
    }

    /**
     * Union/alternation, cached version
     * @return the smart union of this and another regular expression.
     */
    private val unionCache = Memoize<Pair<RegularLanguage, RegularLanguage>, RegularLanguage>(HashMap()) { (c1, c2) ->
        when {
            c1.rejectsAll -> c2
            c2.rejectsAll -> c1
            c1 mustBeSubsumedBy c2 -> c2
            c2 mustBeSubsumedBy c1 -> c1
            else -> Union(c1, c2)
        }
    }

    /**
     * Union/alternation
     * @return the smart union of this and another regular expression from cache if available or compute if absent.
     */
    infix fun `||`(choice2: RegularLanguage): RegularLanguage = unionCache(this to choice2)
}

/**
 * A regular expression that matches the end of the input.
 */
object END : RegularLanguage() {
    override infix fun derive(c: Char): EmptySet = EmptySet
    override fun deriveEND(): Epsilon = Epsilon
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "$$$"
}

/**
 * A regular expression that matches no strings at all.
 */
object EmptySet : RegularLanguage() {
    override infix fun derive(c: Char): EmptySet = this
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = true
    override val isEmptyString: Boolean = false
    override fun toString(): String = "{}"
}

/**
 * A regular expression that matches the empty string.
 */
object Epsilon : RegularLanguage() {
    override infix fun derive(c: Char): EmptySet = EmptySet
    override val acceptsEmptyString: Boolean = true
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = true
    override fun toString(): String = "e"
}

/**
 * A regular expression that matches any character.
 */
object AnyChar : RegularLanguage() {
    override infix fun derive(c: Char): Epsilon = Epsilon
    override val acceptsEmptyString: Boolean = false
    override val rejectsAll: Boolean = false
    override val isEmptyString: Boolean = false
    override fun toString(): String = "."
}

/**
 * A regular expression that matches a specific character.
 */
data class Character(private val ch: Char) : RegularLanguage() {
    override infix fun derive(c: Char): RegularLanguage =
            if (c == ch) Epsilon
            else EmptySet

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
            if (set.contains(c)) Epsilon
            else EmptySet

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
            if (set.contains(c)) EmptySet
            else Epsilon

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
            if (n <= 0) Epsilon
            else (regex derive c) `~` (regex `^` (n - 1))

    override val acceptsEmptyString: Boolean
        get() = (n == 0) || ((n > 0) && regex.acceptsEmptyString)
    override val rejectsAll: Boolean
        get() = (n < 0) || regex.rejectsAll
    override val isEmptyString: Boolean
        get() = (n == 0) || ((n > 0) && regex.isEmptyString)
}
