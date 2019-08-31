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

package klisp.parser.lexer.lang

import klisp.Memoize

/**
 * A regular language is the set of strings constructed from union,
 * concatenation and repetition.
 */
@Suppress("unused")
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
    open fun deriveEND(): RegularLanguage = NO_MATCH

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
            c1 is Character && c2 is ANY_CHAR -> true
            else -> false
        }
    }

    /**
     * Kleene star; zero or more repetitions
     * @return zero or more repetitions of this regular expression.
     */
    val ZERO_OR_MORE: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> EMPTY
            else -> Star(this)
        }

    /**
     * Exactly n repetitions
     */
    infix fun EXACTLY(n: Int): RegularLanguage = when {
        n < 0 -> NO_MATCH
        n == 0 -> EMPTY
        n == 1 -> this
        isEmptyString -> EMPTY
        rejectsAll -> NO_MATCH
        else -> Repetition(this, n)
    }

    /**
     * @return one or more repetitions of this regular expression.
     */
    val ONE_OR_MORE: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> NO_MATCH
            else -> this CONCAT Star(this)
        }

    /**
     * @return the option of this regular expression.
     */
    val OPTIONAL: RegularLanguage
        get() = when {
            isEmptyString -> this
            rejectsAll -> EMPTY
            else -> EMPTY UNION this
        }

    /**
     * Concatenation
     * @return the smart concatenation of this and another regular expression.
     */
    infix fun CONCAT(suffix: RegularLanguage): RegularLanguage = when {
        isEmptyString -> suffix
        suffix.isEmptyString -> this
        rejectsAll -> NO_MATCH
        suffix.rejectsAll -> NO_MATCH
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
    infix fun UNION(choice2: RegularLanguage): RegularLanguage = unionCache(this to choice2)
}