@file:Suppress("unused")

package klisp.parser.derivative

import klisp.SortedSet
import klisp.cons
import klisp.subsetOf

/**
 * A context-free pattern describes a language (a set of strings) with
 * recursive structure.
 *
 * It's not inaccurate (even if it is uncommon) to think of context-free
 * grammars as recursive regular expressions.
 */
abstract class Pattern {
    /**
     * @return the concatenation of this pattern with <code>pat2</code>.
     */
    infix fun `~`(pat2: Pattern) = ConPattern(this, pat2)

    /**
     * @return the alternation of this pattern with <code>pat2</code>.
     */
    infix fun `|`(pat2: Pattern) = AltPattern(this, pat2)

    /**
     * @return the simplified concatenation of this pattern with <code>pat2</code>.
     */
    infix fun `~~`(pat2: Pattern): Pattern = when {
        this.empty || pat2.empty -> EmptyPattern
        this == Eps -> pat2
        pat2 == Eps -> this
        else -> ConPattern(this, pat2)
    }

    /**
     * @return the simplified alternation of this pattern with <code>pat2</code>.
     */
    infix fun `||`(pat2: Pattern): Pattern = when {
        this.empty -> pat2
        pat2.empty -> this
        else -> AltPattern(this, pat2)
    }

    /**
     * A cache of local derivatives.
     */
    private val derivatives =
            mutableMapOf<TokenTag, Pattern>()

    /**
     * Takes the derivative and caches the result.
     * @param c the token tag with respect to which the derivative is taken.
     * @return the derivative with respect to <code>c</code>.
     */
    open infix fun derive(c: TokenTag): Pattern =
            derivatives[c] ?: this derivative c

    /**
     * Specific patterns need to define this method.
     * @return the specific derivative of this pattern.
     */
    protected abstract infix fun derivative(c: TokenTag): Pattern

    /**
     * @return the set of tokens which could appear first in this pattern.
     */
    val first: SortedSet<TokenPattern> = attributes.first.value

    /**
     * @return the set of tokens which could appear first in this pattern,
     * ignoring parsing markers.
     */
    val firstc: SortedSet<TokenPattern> = attributes.firstc.value

    /**
     * @return true iff this pattern accepts the empty string.
     */
    val nullable: Boolean = attributes.nullable.value

    /**
     * @return true iff this pattern accepts the empty string, treating parsing markers as empty.
     */
    val nullablec: Boolean = attributes.nullablec.value

    /**
     * @return true iff this pattern accepts no strings.
     */
    val empty: Boolean = attributes.empty.value

    /**
     * A collection of attributes which must be computed by iteration to a fixed point.
     */
    object attributes {
        private var generation = -1
        private var stabilized = false

        /**
         * An attribute computable by fixed point.
         *
         * @param bottom the bottom of the attribute's lattice.
         * @param join the lub operation on the lattice.
         * @param wt the partial order on the lattice.
         */
        abstract class Attribute<A>(private val bottom: A,
                                    private val join: (A, A) -> A,
                                    private val wt: (A, A) -> Boolean) {

            private var currentValue: A = bottom
            private lateinit var compute: () -> A
            private var fixed = false

            /**
             * Sets the computation that updates this attribute.
             *
             * @param computation the computation that updates this attribute.
             */
            infix fun `|=`(computation: () -> A) {
                compute = { computation() }
            }

            /**
             * Permanently fixes the value of this attribute.
             *
             * @param value the value of this attribute.
             */
            infix fun `|==`(value: A) {
                currentValue = value
                fixed = true
            }

            /**
             * Recomputes the value of this attribute.
             */
            fun update() {
                if (fixed) return
                val newValue = compute()
                if (!wt(newValue, currentValue)) {
                    currentValue = join(newValue, currentValue)
                    FixedPoint.changed = true
                }
            }

            /**
             * The current value of this attribute.
             */
            val value: A
                get() {
                    // When the value of this attribute is requested, there are
                    // three possible cases:
                    //
                    // (1) It's already been computed (this.stabilized);
                    // (2) It's been manually set (this.fixed); or
                    // (3) It needs to be computed (generation < FixedPoint.generation).
                    if (fixed || stabilized || (generation == FixedPoint.generation))
                        return currentValue
                    else {
                        // Run or continue the fixed-point computation:
                        fix()
                    }

                    if (FixedPoint.stabilized)
                        stabilized = true

                    return currentValue
                }
        }

        // Subsumption tests for attributes:

        private fun implies(a: Boolean, b: Boolean) = (!a) || b
        private fun follows(a: Boolean, b: Boolean) = (!b) || a
        private fun subsetOf(a: Set<TokenPattern>, b: Set<TokenPattern>) = a subsetOf b

        object first : Attribute<SortedSet<TokenPattern>>(SortedSet(), { a, b -> SortedSet(a + b) }, ::subsetOf)
        object firstc : Attribute<SortedSet<TokenPattern>>(SortedSet(), { a, b -> SortedSet(a + b) }, ::subsetOf)
        object nullable : Attribute<Boolean>(false, { a, b -> a || b }, ::implies)
        object nullablec : Attribute<Boolean>(false, { a, b -> a || b }, ::implies)
        object empty : Attribute<Boolean>(true, { a, b -> a && b }, ::follows)

        private fun updateAttributes() {
            nullable.update()
            nullablec.update()
            empty.update()
            first.update()
            firstc.update()
        }

        private fun fix() {
            generation = FixedPoint.generation
            if (FixedPoint.master === null) {
                do {
                    FixedPoint.generation += 1
                    FixedPoint.changed = false
                    updateAttributes()
                } while (FixedPoint.changed)
                FixedPoint.stabilized = true
                FixedPoint.generation += 1
                updateAttributes()
                FixedPoint.reset()
            } else updateAttributes()
        }
    }
}

/**
 * FixedPoint tracks the state of a fixed point algorithm for the attributes of a grammar.
 *
 * In case there are fixed points running in multiple threads, each attribute is thread-local.
 */
private object FixedPoint {
    var stabilized = false
    var running = false
    var changed = false
    var generation = 0
    var master: Any? = null

    fun reset() {
        stabilized = false
        running = false
        master = null
        changed = false
        generation = 0
    }
}

/**
 * Represents the union of two context-free patterns.
 */
data class AltPattern(private val pat1: Pattern, private val pat2: Pattern) : Pattern() {
    init {
        attributes.nullable `|=` { pat1.nullable || pat2.nullable }
        attributes.nullablec `|=` { pat1.nullablec || pat2.nullablec }
        attributes.empty `|=` { pat1.empty || pat2.empty }
        attributes.first `|=` { SortedSet(pat1.first + pat2.first) }
        attributes.firstc `|=` { SortedSet(pat1.firstc + pat2.firstc) }
    }

    override fun derivative(c: TokenTag): Pattern =
            pat1 derive c `||` pat2 derive c

    override fun hashCode(): Int = 2 * pat1.hashCode() + pat2.hashCode()
    override fun toString(): String = "$pat1 | $pat2"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AltPattern
        if (pat1 != other.pat1) return false
        if (pat2 != other.pat2) return false
        return true
    }
}

/**
 * Represents the concatenation of two context-free patterns.
 */
data class ConPattern(private val pat1: Pattern, private val pat2: Pattern) : Pattern() {
    init {
        attributes.nullable `|=` { pat1.nullable && pat2.nullable }
        attributes.nullablec `|=` { pat1.nullablec && pat2.nullablec }
        attributes.empty `|=` { pat1.empty || pat2.empty }
        attributes.first `|=` {
            if (pat1.nullable) SortedSet(pat1.first + pat2.first)
            else pat1.first
        }
        attributes.firstc `|=` {
            if (pat1.nullablec) SortedSet(pat1.firstc + pat2.firstc)
            else pat1.firstc
        }
    }

    override fun derivative(c: TokenTag): Pattern =
            if (pat1.nullable)
                pat1 derive c `~~` pat2 `||` pat2 derive c
            else
                pat1 derive c `~~` pat2

    override fun hashCode(): Int = 2 * pat1.hashCode() + pat2.hashCode()
    override fun toString(): String = "$pat1 ~ $pat2"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ConPattern
        if (pat1 != other.pat1) return false
        if (pat2 != other.pat2) return false
        return true
    }
}

/**
 * Represents zero or more repetitions of a context-free pattern.
 */
data class RepPattern(private val pat: Pattern) : Pattern() {
    init {
        attributes.nullable `|==` true
        attributes.nullablec `|==` true
        attributes.empty `|==` false
        attributes.first `|=` { pat.first }
        attributes.firstc `|=` { pat.firstc }
    }

    override fun derivative(c: TokenTag): Pattern = pat derive c `~~` this
    override fun hashCode(): Int = pat.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as RepPattern
        if (pat != other.pat) return false
        return true
    }
}

/**
 * Represents zero or one instances of the supplied pattern.
 */
data class OptPattern(private val pat: Pattern) : Pattern() {
    init {
        attributes.nullable `|==` true
        attributes.nullablec `|==` true
        attributes.empty `|==` false
        attributes.first `|=` { pat.first }
        attributes.firstc `|=` { pat.firstc }
    }

    override fun derivative(c: TokenTag): Pattern =
            pat derive c

    override fun hashCode(): Int = pat.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as OptPattern
        if (pat != other.pat) return false
        return true
    }
}

/**
 * Represents a pattern that matches a token with a particular token
 * tag.  Every token carries a "tag" indicating its type.  For most
 * tokens, their tag will be a string.
 *
 * @param tag the tag to match.
 * @param isParsingMarker true iff this is a parsing marker tag.
 */
data class TokenPattern(val tag: TokenTag,
                        val isParsingMarker: Boolean)
    : Pattern(), Comparable<TokenPattern> {

    constructor(token: Token) : this(token.tag, token.isParsingMarker)

    init {
        attributes.nullable `|==` false
        attributes.nullablec `|==` this.isParsingMarker
        attributes.empty `|==` false
        attributes.first `|==` SortedSet(this)
        attributes.firstc `|==`
                if (this.isParsingMarker)
                    SortedSet()
                else
                    SortedSet(this)
    }

    /**
     * @return true iff this pattern matches the given token tag.
     */
    infix fun matches(c: TokenTag): Boolean = tag == c

    override fun derivative(c: TokenTag): Pattern =
            if (this matches c) Eps
            else EmptyPattern

    override fun compareTo(other: TokenPattern): Int =
            tag.toString().compareTo(other.tag.toString())

    override fun toString(): String = tag.toString()
    override fun hashCode(): Int = tag.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TokenPattern
        if (tag != other.tag) return false
        if (isParsingMarker != other.isParsingMarker) return false
        return true
    }
}

/**
 * A pattern representing the language with only the empty string in it.
 */
object Eps : Pattern() {
    init {
        attributes.nullable `|==` true
        attributes.nullablec `|==` true
        attributes.first `|==` SortedSet()
        attributes.firstc `|==` SortedSet()
        attributes.empty `|==` false
    }

    override fun derivative(c: TokenTag): Pattern = EmptyPattern
    override fun derive(c: TokenTag): Pattern = EmptyPattern
    override fun hashCode(): Int = 1
}

/**
 * A pattern representing language with no strings in it; that is, this
 * is the unmatchable pattern.
 */
object EmptyPattern : Pattern() {
    init {
        attributes.nullable `|==` false
        attributes.nullablec `|==` false
        attributes.first `|==` SortedSet()
        attributes.firstc `|==` SortedSet()
        attributes.empty `|==` true
    }

    override fun derivative(c: TokenTag): Pattern = EmptyPattern
    override fun hashCode(): Int = 2
}

/**
 * A generic pattern corresponds to a nonterminal in a context-free grammar.
 *
 * A generic pattern can be extended with the ::= operation.
 */
open class GenericPattern : Pattern() {
    companion object {
        private var id = 0
        /**
        @return the next available id for a generic pattern.
         */
        val nextId: Int
            get() {
                id += 1
                return id
            }
    }

    private var _rules = emptyList<Pattern>()
    /**
     * The rules that define this pattern.
     */
    open val rules get() = _rules

    /**
     * Sets the rules that define this pattern.
     */
    infix fun `||=`(r: Pattern) {
        _rules = r cons _rules
    }

    /**
     * The unique id of this pattern.
     */
    val id = nextId

    override infix fun derivative(c: TokenTag): GenericPattern = Derivative(this, c)

    init {
        attributes.nullable `|=` { rules.any(Pattern::nullable) }
        attributes.nullablec `|=` { rules.any(Pattern::nullablec) }
        attributes.empty `|=` { rules.all(Pattern::empty) }
        attributes.first `|=` { rules.fold(SortedSet()) { t, p -> SortedSet(t + p.first) } }
        attributes.firstc `|=` { rules.fold(SortedSet()) { t, p -> SortedSet(t + p.firstc) } }
    }

    override fun equals(other: Any?): Boolean =
            if (other is GenericPattern) other.id == this.id
            else false

    override fun hashCode(): Int = id
    override fun toString(): String = "N$id"
}

/**
 * Represents the derivative of a generic pattern.
 *
 * @param base the core pattern.
 * @param c the tag with respect to which the derivative is taken.
 */
data class Derivative(private val base: GenericPattern, private val c: TokenTag) : GenericPattern() {
    override val rules by lazy { base.rules.map { it derive c } }

    override fun hashCode(): Int = 2 * base.hashCode() + c.hashCode()
    override fun toString(): String = "D{$c}"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        if (!super.equals(other)) return false
        other as Derivative
        if (base != other.base) return false
        if (c != other.c) return false
        return true
    }
}
