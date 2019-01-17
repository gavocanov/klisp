@file:Suppress("unused", "NonAsciiCharacters")

package klisp.parser.derivative

import klisp.FP
import klisp.IFP
import klisp.None
import klisp.Option
import klisp.Some
import klisp.SortedSet
import klisp.cons
import klisp.exIfNull
import klisp.head
import klisp.reversed
import klisp.subsetOf
import klisp.tail
import klisp.toListOf
import klisp.unApply
import kotlin.jvm.Synchronized

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
    open infix fun derive(c: TokenTag): Pattern {
        val cached = derivatives[c]
        return if (cached !== null) cached
        else {
            val d = this.derivative(c)
            derivatives[c] = d
            d
        }
    }

    /**
     * Specific patterns need to define this method.
     * @return the specific derivative of this pattern.
     */
    protected abstract infix fun derivative(c: TokenTag): Pattern

    /**
     * @return the set of tokens which could appear first in this pattern.
     */
    val first: SortedSet<TokenPattern> get() = attributes.first.value

    /**
     * @return the set of tokens which could appear first in this pattern,
     * ignoring parsing markers.
     */
    val firstc: SortedSet<TokenPattern> get() = attributes.firstc.value

    /**
     * @return true iff this pattern accepts the empty string.
     */
    val nullable: Boolean get() = attributes.nullable.value

    /**
     * @return true iff this pattern accepts the empty string, treating parsing markers as empty.
     */
    val nullablec: Boolean get() = attributes.nullablec.value

    /**
     * @return true iff this pattern accepts no strings.
     */
    val empty: Boolean get() = attributes.empty.value

    /**
     * An attribute computable by fixed point.
     *
     * @param bottom the bottom of the attribute's lattice.
     * @param join the lub operation on the lattice.
     * @param wt the partial order on the lattice.
     */
    abstract class Attribute<A>(private val bottom: A,
                                private val join: (A, A) -> A,
                                private val wt: (A, A) -> Boolean,
                                private val attributes: Attributes) {

        private var currentValue: A = bottom
        private var compute: () -> A = { throw IllegalStateException("should not be here") }
        private var fixed = false

        /**
         * Sets the computation that updates this attribute.
         *
         * @param computation the computation that updates this attribute.
         */
        infix fun `ː=`(computation: () -> A) {
            val a = computation
            compute = a
        }

        /**
         * Permanently fixes the value of this attribute.
         *
         * @param value the value of this attribute.
         */
        infix fun `ː==`(value: A) {
            currentValue = value
            fixed = true
        }

        /**
         * Recomputes the value of this attribute.
         */
        fun update() {
            if (fixed)
                return

            val newValue = compute()

            if (!wt(newValue, currentValue)) {
                currentValue = join(newValue, currentValue)
                FixedPoint.changed(true)
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
                if (fixed || attributes.stabilized || (attributes.generation == FixedPoint.generation()))
                    return currentValue
                else {
                    // Run or continue the fixed-point computation:
                    attributes.fix()
                }

                if (FixedPoint.stabilized())
                    attributes.stabilized = true

                return currentValue
            }
    }

    interface Attributes {
        var generation: Int
        var stabilized: Boolean
        val first: Attribute<SortedSet<TokenPattern>>
        val firstc: Attribute<SortedSet<TokenPattern>>
        val nullable: Attribute<Boolean>
        val nullablec: Attribute<Boolean>
        val empty: Attribute<Boolean>
        fun fix()
    }

    /**
     * A collection of attributes which must be computed by iteration to a fixed point.
     */
    protected val attributes = object : Attributes {
        override var generation = -1
        override var stabilized = false

        // Subsumption tests for attributes:

        private fun implies(a: Boolean, b: Boolean) =
                (!a) || b

        private fun follows(a: Boolean, b: Boolean) =
                (!b) || a

        private fun subsetOf(a: Set<TokenPattern>, b: Set<TokenPattern>) =
                a subsetOf b

        override val first = object : Attribute<SortedSet<TokenPattern>>(
                bottom = SortedSet(),
                join = { a, b -> SortedSet(a + b) },
                wt = ::subsetOf,
                attributes = this
        ) {}

        override val firstc = object : Attribute<SortedSet<TokenPattern>>(
                bottom = SortedSet(),
                join = { a, b -> SortedSet(a + b) },
                wt = ::subsetOf,
                attributes = this
        ) {}

        override val nullable = object : Attribute<Boolean>(
                bottom = false,
                join = { a, b -> a || b },
                wt = ::implies,
                attributes = this
        ) {}

        override val nullablec = object : Attribute<Boolean>(
                bottom = false,
                join = { a, b -> a || b },
                wt = ::implies,
                attributes = this
        ) {}

        override val empty = object : Attribute<Boolean>(
                bottom = true,
                join = { a, b -> a && b },
                wt = ::follows,
                attributes = this
        ) {}

        private fun updateAttributes() {
            nullable.update()
            nullablec.update()
            empty.update()
            first.update()
            firstc.update()
        }

        override fun fix() {
            this.generation = FixedPoint.generation()
            if (FixedPoint.master() === null) {
                FixedPoint.master(this)
                do {
                    FixedPoint.incGeneration()
                    FixedPoint.changed(false)
                    updateAttributes()
                } while (FixedPoint.changed())
                FixedPoint.stabilized(true)
                FixedPoint.incGeneration()
                updateAttributes()
                FixedPoint.reset()
            } else {
                updateAttributes()
            }
        }
    }
}

/**
 * FixedPoint tracks the state of a fixed point algorithm for the attributes of a grammar.
 *
 * In case there are fixed points running in multiple threads, each attribute is thread-local.
 */
private val FixedPoint: IFP = FP()

/**
 * Represents the union of two context-free patterns.
 */
data class AltPattern(private val pat1: Pattern, private val pat2: Pattern) : Pattern() {
    override fun derivative(c: TokenTag): Pattern =
            pat1 derive c `||` pat2 derive c

    init {
        attributes.nullable `ː=` { pat1.nullable || pat2.nullable }
        attributes.nullablec `ː=` { pat1.nullablec || pat2.nullablec }
        attributes.empty `ː=` { pat1.empty && pat2.empty }
        attributes.first `ː=` { SortedSet(pat1.first + pat2.first) }
        attributes.firstc `ː=` { SortedSet(pat1.firstc + pat2.firstc) }
    }
}

/**
 * Represents the concatenation of two context-free patterns.
 */
data class ConPattern(private val pat1: Pattern, private val pat2: Pattern) : Pattern() {
    override fun derivative(c: TokenTag): Pattern =
            if (pat1.nullable)
                pat1 derive c `~~` pat2 `||` pat2 derive c
            else
                pat1 derive c `~~` pat2

    init {
        attributes.nullable `ː=` { pat1.nullable && pat2.nullable }
        attributes.nullablec `ː=` { pat1.nullablec && pat2.nullablec }
        attributes.empty `ː=` { pat1.empty || pat2.empty }
        attributes.first `ː=` {
            if (pat1.nullable) SortedSet(pat1.first + pat2.first)
            else pat1.first
        }
        attributes.firstc `ː=` {
            if (pat1.nullablec) SortedSet(pat1.firstc + pat2.firstc)
            else pat1.firstc
        }
    }
}

/**
 * Represents zero or more repetitions of a context-free pattern.
 */
data class RepPattern(private val pat: Pattern) : Pattern() {
    override fun derivative(c: TokenTag): Pattern = pat derive c `~~` this

    init {
        attributes.nullable `ː==` true
        attributes.nullablec `ː==` true
        attributes.empty `ː==` false
        attributes.first `ː=` { pat.first }
        attributes.firstc `ː=` { pat.firstc }
    }
}

/**
 * Represents zero or one instances of the supplied pattern.
 */
data class OptPattern(private val pat: Pattern) : Pattern() {
    override fun derivative(c: TokenTag): Pattern =
            pat derive c

    init {
        attributes.nullable `ː==` true
        attributes.nullablec `ː==` true
        attributes.empty `ː==` false
        attributes.first `ː=` { pat.first }
        attributes.firstc `ː=` { pat.firstc }
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

    /**
     * @return true iff this pattern matches the given token tag.
     */
    infix fun matches(c: TokenTag): Boolean = tag == c

    private val tagString: String by lazy { tag.toString() }

    override fun derivative(c: TokenTag): Pattern =
            if (this matches c) Eps
            else EmptyPattern

    override fun compareTo(other: TokenPattern): Int =
            tagString.compareTo(other.tagString)

    init {
        attributes.nullable `ː==` false
        attributes.nullablec `ː==` this.isParsingMarker
        attributes.empty `ː==` false
        attributes.first `ː==` SortedSet(this)
        attributes.firstc `ː==`
                if (this.isParsingMarker)
                    SortedSet()
                else
                    SortedSet(this)
    }
}

/**
 * A pattern representing the language with only the empty string in it.
 */
object Eps : Pattern() {
    override fun derivative(c: TokenTag): EmptyPattern = EmptyPattern
    override fun derive(c: TokenTag): EmptyPattern = EmptyPattern
    override fun hashCode(): Int = 1

    init {
        attributes.nullable `ː==` true
        attributes.nullablec `ː==` true
        attributes.first `ː==` SortedSet()
        attributes.firstc `ː==` SortedSet()
        attributes.empty `ː==` false
    }
}

/**
 * A pattern representing language with no strings in it; that is, this
 * is the unmatchable pattern.
 */
object EmptyPattern : Pattern() {
    override fun derivative(c: TokenTag): EmptyPattern = EmptyPattern
    override fun hashCode(): Int = 2

    init {
        attributes.nullable `ː==` false
        attributes.nullablec `ː==` false
        attributes.first `ː==` SortedSet()
        attributes.firstc `ː==` SortedSet()
        attributes.empty `ː==` true
    }
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
            @Synchronized
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
    infix fun `ːː=`(r: Pattern) {
        _rules = r cons _rules
    }

    /**
     * The unique id of this pattern.
     */
    private val id by lazy { GenericPattern.nextId }

    override infix fun derivative(c: TokenTag): GenericPattern = Derivative(this, c)

    override fun equals(other: Any?): Boolean =
            if (other is GenericPattern) other.id == this.id
            else false

    override fun hashCode(): Int = id
    override fun toString(): String = "N$id"

    init {
        attributes.nullable `ː=` { this.rules.any(Pattern::nullable) }
        attributes.nullablec `ː=` { this.rules.any(Pattern::nullablec) }
        attributes.empty `ː=` { this.rules.all(Pattern::empty) }
        attributes.first `ː=` { this.rules.fold(SortedSet()) { t, p -> SortedSet(t + p.first) } }
        attributes.firstc `ː=` { this.rules.fold(SortedSet()) { t, p -> SortedSet(t + p.firstc) } }
    }
}

/**
 * Represents the derivative of a generic pattern.
 *
 * @param base the core pattern.
 * @param c the tag with respect to which the derivative is taken.
 */
data class Derivative(private val base: GenericPattern, private val c: TokenTag) : GenericPattern() {
    override val rules by lazy {
        base.rules.map { r -> r derive c }
    }

    override fun toString(): String = "D{$c}"
}

/**
 * Parsing markers are special tokens that appear in parse strings, but not in input strings.
 *
 * The parsing machine converts streams of tokens into lists of tokens containing these markers.
 *
 * The markers indicate the structure of the parse.
 */
abstract class ParsingMark : Token() {
    override val isParsingMarker = true
    override val tag: ParsingMark by lazy { this }
}

/**
 * A "hard" empty string (a hard epsilon) is a parsing marker inserted
 * into a parse string to indicate that the original context-free
 * grammar called for parsing the empty string.
 */
object HardEps : ParsingMark() {
    override fun localCompare(other: Token): Int = when (other) {
        is HardEps -> 0
        else -> throw IllegalStateException("this should not be")
    }

    override fun toString() = "[e]"
    override fun hashCode() = 3
}

/**
 * An open reduction marker in a parse string indicates that opening of
 * a new node in the parse tree.
 *
 * @param f  the function to construct the tree node from its leaves.
 * @param id the unique id of this reduction rule.
 */
data class OpenRed(private val f: (Any) -> Any, val id: Int) : ParsingMark() {
    override fun localCompare(other: Token): Int {
        other as OpenRed
        return id.compareTo(other.id)
    }

    override fun toString(): String = "<${f.hashCode()}|"

    override fun equals(other: Any?): Boolean = when (other) {
        is OpenRed -> other.id == this.id
        else -> false
    }

    override fun hashCode(): Int = f.hashCode()
}

/**
 * A close reduction parsing marker in a parse string indicates the
 * end of a node in the parse tree.
 *
 * Open/close reduction markers match like balanced parentheses in a
 * correct parse string.
 */
data class CloseRed(val f: (Any) -> Any, val id: Int) : ParsingMark() {
    override fun localCompare(other: Token): Int {
        other as CloseRed
        return id.compareTo(other.id)
    }

    override fun toString(): String = "|${f.hashCode()}>"

    override fun equals(other: Any?): Boolean = when (other) {
        is CloseRed -> other.id == this.id
        else -> false
    }

    override fun hashCode(): Int = f.hashCode()
}

/**
 * Open/close repetition parsing markers indicate that the nodes between should be converted into a list.
 */
object OpenRep : ParsingMark() {
    override fun localCompare(other: Token): Int = when (other) {
        is OpenRep -> 0
        else -> throw IllegalStateException("this should not be")
    }

    override fun hashCode(): Int = 1
    override fun toString(): String = "<*|"
}

/**
 * Open/close repetition parsing markers indicate that the nodes between should be converted into a list.
 */
object CloseRep : ParsingMark() {
    override fun localCompare(other: Token): Int = when (other) {
        is CloseRep -> 0
        else -> throw IllegalStateException("this should not be")
    }

    override fun hashCode(): Int = 2
    override fun toString(): String = "|*>"
}

/**
 * Open/close option parsing markers indicate that the node between (if
 * any) should be converted to an <code>Option</code>.
 */
object OpenOpt : ParsingMark() {
    override fun localCompare(other: Token): Int = when (other) {
        is OpenOpt -> 0
        else -> throw IllegalStateException("this should not be")
    }

    override fun hashCode(): Int = 1
    override fun toString(): String = "<?|"
}

/**
 * Open/close option parsing markers indicate that the node between (if
 * any) should be converted to an <code>Option</code>.
 */
object CloseOpt : ParsingMark() {
    override fun localCompare(other: Token): Int = when (other) {
        is CloseOpt -> 0
        else -> throw IllegalStateException("this should not be")
    }

    override fun hashCode(): Int = 2
    override fun toString(): String = "|?>"
}

/**
 * A parsing state is a node in the graph explored by a parsing machine.
 *
 * A parsing state indicates a partially parsed input.
 *
 * @param lang  the pattern meant to match the remaining input.
 * @param parse the parse string constructed thus far.
 * @param input the remaining input.
 */
data class ParsingState<T : Token>(val lang: Pattern,
                                   val parse: List<Token>,
                                   val input: LiveStream<T>) {

    private val firstMarks: Set<ParsingMark> by lazy {
        val first = lang.first
        val tokens = SortedSet(first.filter(TokenPattern::isParsingMarker))
        tokens.map { it.tag as ParsingMark }.toSet()
    }

    /**
     * @return true iff this state can consume a parsing marker.
     */
    val hasMarks get() = firstMarks.isNotEmpty()

    /**
     * @return true iff the input consumed thus far is a legal parse.
     */
    val isFinal get() = lang.nullable

    /**
     * @return true iff the input stream in this state is ready to make progress.
     */
    val canConsume: Boolean get() = !input.isPlugged && !lang.empty

    /**
     * @return the state that results from consuming the head of the input, if any.
     */
    val nextConsume: Option<ParsingState<T>>
        get() {
            val (c, rest) = when (val d = LiveHT unapply input) {
                is Some -> d()
                is None -> d.unapply
            }

            return when {
                input.isEmpty -> throw IllegalStateException("can't consume -- end of input")
                input.isPlugged -> throw IllegalStateException("can't consume on a plugged stream")
                else -> {
                    c as T
                    rest as LiveStream<T>
                    val newLang = lang derive c.tag
                    val newParseString: List<Token> = c cons parse

                    if (!newLang.empty)
                        Some(ParsingState(newLang, newParseString, rest))
                    else
                        None
                }
            }
        }
    /**
     * @return the next states resulting from consuming all possible
     *         parsing marks at the start of the language.
     */
    val nextMark: List<ParsingState<T>>
        get() {
            val marks = firstMarks
            return if (input.isPlugged) {
                // If the input is plugged; derive.
                marks.map { c ->
                    val newLang = lang derive c
                    val newParseString = c cons parse
                    ParsingState(newLang, newParseString, input)
                }
            } else {
                // If the input isn't plugged, eliminate subsequent states which
                // can't ever match the head of the input.
                val canMatchHead = { pats: Set<TokenPattern> ->
                    if (input.isEmpty) true
                    else pats.any { pat -> pat matches input.head.tag }
                }
                marks
                        .filter { c ->
                            (lang derive c).nullable || canMatchHead((lang derive c).firstc)
                        }
                        .map { c ->
                            val newLang = lang derive c
                            val newParseString = c cons parse
                            ParsingState(newLang, newParseString, input)
                        }
            }
        }

    override fun toString(): String =
            "lang: $lang\nparse: ${parse.reversed}\ninput: $input"

    /**
     * Takes a step toward converting a parse string into a parse tree.
     */
    @Suppress("UNCHECKED_CAST")
    private fun reduceStep(stack: List<Any?>, parseString: List<Token>): Pair<List<Any?>, List<Token>> {
        fun combine(newTop: Any?, _stack: List<Any?>, rest: List<Token>): Pair<List<Any?>, List<Token>> {
            val (hd, _tl) = _stack unApply 2
            val tl = _tl as List<Any?>
            return when {
                hd is CloseRed || hd is CloseRep || hd is CloseOpt ->
                    ((newTop cons hd) cons tl) to rest
                _stack.isEmpty() ->
                    newTop.toListOf() to rest
                else ->
                    (Seq(newTop, hd) cons tl) to rest
            }
        }

        fun reassociate(data: Any?): Any {
            if (data is Seq<*, *> && data.second is Seq<*, *>) {
                val a = data.first
                val _b = data.second as Seq<Any?, Any?>
                val b = _b.first
                val rest = _b.second
                return reassociate(Seq(Seq(a, b), rest))
            }
            return data.exIfNull
        }

        val (sh, s2, _s3t) = stack unApply 3
        val (_, _s2t) = stack unApply 2
        val st2 = _s2t as List<Any?>
        val st3 = _s3t as List<Any?>

        val (_ph, _pt) = parseString unApply 2
        val ph = _ph.exIfNull as Token
        val pt = _pt as List<Token>

        val r = when {
            ph is CloseRed ->
                ph cons stack to pt
            ph is CloseRep ->
                ph cons stack to pt
            ph is CloseOpt ->
                ph cons stack to pt
            ph is HardEps ->
                combine(Unit, stack, pt)
            s2 is CloseRed && ph is OpenRed && (s2.id == ph.id) -> {
                val rData = reassociate(sh)
                combine(s2.f(rData), st3, pt)
            }
            sh is CloseRep && ph is OpenRep ->
                combine(emptyList<Any?>(), st2, pt)
            s2 is CloseRep && ph is OpenRep ->
                combine(listify(sh.exIfNull), st3, pt)
            sh is CloseOpt && ph is OpenOpt ->
                combine(None, st2, pt)
            s2 is CloseOpt && ph is OpenOpt ->
                combine(Some(sh), st3, pt)
            sh is CloseRed || sh is CloseRep || sh is CloseOpt ->
                combine(ph, sh cons st2, pt)
            stack.isNotEmpty() && parseString.isEmpty() ->
                throw IllegalStateException("bug, over-parsed")
            else ->
                combine(Seq(ph, sh), st2, pt)
        }

        return r
    }

    private fun listify(o: Any): List<Any> = when (o) {
        is Seq<*, *> -> o.first.exIfNull cons listify(o.second.exIfNull)
        else -> emptyList()
    }

    /**
     * Converts a parse string into a parse tree.
     */
    fun reduce(): Any {
        var state: Pair<List<Any?>, List<Token>> = emptyList<Any?>() to parse
        while (state.second.isNotEmpty()) {
            state = reduceStep(state.first, state.second)
        }
        return state.first.head.exIfNull
    }
}

/**
 * A parsing machine parses by exploring the graph of parsing states.
 *
 * The exploration uses two work-lists: a high-priority list, and a low-priority list.
 *
 * The exploration only pulls from the low-priority list when the high-priority list is empty.
 *
 * The high-priority states should consume a character; the low-priority
 * states should consume a parsing mark.
 */
class ParsingMachine<T : Token>(private val lang: Pattern, private val input: LiveStream<T>) {
    /**
     * High-priority to-do list for configurations which could consume a character.
     */
    private var highTodo: List<ParsingState<T>> =
            listOf(ParsingState(lang, emptyList(), input))

    /**
     * Low-priority to-do list for configurations which might produce a parsing marker.
     */
    private var lowTodo: List<ParsingState<T>> =
            listOf(ParsingState(lang, emptyList(), input))

    private val finalSource = LiveStreamSource<ParsingState<T>>()

    /**
     * A stream of final parsing states; final parsing states can be reduced.
     */
    val output: LiveStream<ParsingState<T>> = LiveStream(finalSource)

    /**
     * Returns the newest frontier states in the parsing state search
     * space.
     */
    private fun nextStates(): Iterable<ParsingState<T>>? = when {
        highTodo.isNotEmpty() && highTodo.head.canConsume -> {
            val next = highTodo.head
            highTodo = highTodo.tail
            next.nextConsume
        }
        lowTodo.isNotEmpty() && !lowTodo.head.lang.empty -> {
            val next = lowTodo.head
            lowTodo = lowTodo.tail
            next.nextMark
        }
        else -> null
    }

    /**
     * Searches the parsing state-space as much as possible given the input available.
     */
    private fun search() {
        while (highTodo.isNotEmpty() || lowTodo.isNotEmpty()) {
            val newConfs = nextStates()

            highTodo += (newConfs?.filter { it.lang.firstc.isNotEmpty() }) ?: emptyList()
            lowTodo += (newConfs?.filter { it.hasMarks }) ?: emptyList()

            var foundFinal = false
            newConfs?.forEach { conf ->
                if (conf.isFinal) {
                    finalSource += conf
                    foundFinal = true
                }
            }
            // Pause search when we've found at least one final state
            if (foundFinal) return
        }
    }

    init {
        input.source.addListener { search() }
        // In case any input is ready, search:
        search()
    }
}

/* Parser combinators. */

/**
 * Contains a sequence of two parsed items.
 */
data class Seq<A, B>(val first: A, val second: B) {
    override fun toString(): String = "~($first, $second)"
}

/**
 * An abstract parser that creates parse trees of type <code>A</code>.
 */
abstract class Parser<A> {
    /**
     * @return a new parser which parses the concatenation of this parser and the supplied parser.
     */
    infix fun <B> `~`(pat2: Parser<B>): Parser<Seq<A, B>> = ConParser(this, pat2)

    /**
     * @return a new parser which parses as this parser or the supplied parser.
     */
    infix fun `|`(pat2: Parser<A>): Parser<A> = AltParser(this, pat2)

    /**
     * @return a new parser which accepts zero or more repetitions of this parser.
     */
    val `*`: Parser<List<A>> get() = RepParser(this)

    /**
     * @return a parser which may parse what this parser parses.
     */
    val `?`: Parser<Option<A>> get() = OptParser(this)

    /**
     * @return a parser that parses what this parser parses, but converts the result.
     */
    infix fun <B> `==▷`(f: (A) -> B) = RedParser(this, f)

    /**
     * @return a context-free pattern that matches the parse strings describe by this parser.
     */
    abstract val compile: Pattern

    /**
     * @return a parsing machine for this parser on the specified input.
     */
    private fun <T : Token> machine(input: LiveStream<T>): ParsingMachine<T> =
            ParsingMachine(this.compile, input)

    /**
     * Returns the parse tree from completely consuming the input for this parse.
     *
     * This procedure assumes the parser is unambiguous.
     *
     * If you need access to all possible parse trees, use the <code>machine</code> method.
     *
     * @return the parse tree from completely consuming the input for this parse.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Token> parseFull(input: LiveStream<T>): A {
        val m = machine(input)
        val finals = m.output
        val state = finals.head
        return if (state.input.isEmpty) state.reduce() as A
        else throw IllegalStateException("full parse failure; input remaining: ${state.input}")
    }

    fun <T : Token> parseFull(input: Iterable<T>): A =
            parseFull(LiveStream(input))

    /**
     * Returns the parse tree and the remaining input.
     *
     * This procedure assumes the parser is unambiguous.
     *
     * If you need access to all possible parse trees, use the <code>machine</code> method.
     *
     * @return the parse tree and the remaining input.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Token> parse(input: LiveStream<T>): Pair<A, LiveStream<T>> {
        val m = machine(input)
        val finals = m.output
        val state = finals.head
        return (state.reduce() as A) to state.input
    }

    fun <T : Token> parse(input: Iterable<T>): Pair<A, LiveStream<T>> =
            parse(LiveStream(input))
}

/**
 * Represents an extensible parser that produces parse trees of type <code>A</code>.
 */
open class GenericParser<A> : Parser<A>() {
    private var rules: List<Parser<A>> = emptyList()

    /**
     * Adds a new parser to this parser.
     */
    infix fun `ːː=`(parser: Parser<A>) {
        rules = parser cons rules
    }

    private var compileCache: GenericPattern? = null

    /**
     * Compiles a parser into context-free pattern, such that when the context-free pattern
     * is given to a parsing machine, the parsing machine produces parse trees.
     */
    override val compile: GenericPattern
        get() {
            if (compileCache !== null)
                return compileCache.exIfNull

            compileCache = GenericPattern()
            rules.forEach { pat ->
                val cc = compileCache.exIfNull
                cc `ːː=` pat.compile
            }

            return compileCache.exIfNull
        }
}

/**
 * Matches tokens with the specified tag.
 */
data class TokenParser(val tag: String) : Parser<Token>() {
    override val compile: Pattern
            by lazy { TokenPattern(tag = tag, isParsingMarker = false) }
}

/**
 * Matches strings of length 0.
 */
object EpsParser : Parser<Unit>() {
    override val compile: Pattern
            by lazy { TokenPattern(HardEps) }
}

/**
 * Cannot match any strings.
 */
object EmptyParser : Parser<Nothing>() {
    override val compile: Pattern
            by lazy { EmptyPattern }
}

/**
 * A parser representing the concatenation of two parsers.
 */
data class ConParser<A, B>(private val pat1: Parser<A>,
                           private val pat2: Parser<B>) : Parser<Seq<A, B>>() {
    override val compile: Pattern
            by lazy { ConPattern(pat1.compile, pat2.compile) }
}

/**
 * A parser representing the union of two parsers.
 */
data class AltParser<A>(private val pat1: Parser<A>,
                        private val pat2: Parser<A>) : Parser<A>() {
    override val compile: Pattern by lazy {
        AltPattern(pat1.compile, pat2.compile)
    }
}

/**
 * A parser representing the possibly-empty repetition of another parser.
 */
data class RepParser<A>(private val pat: Parser<A>) : Parser<List<A>>() {
    override val compile: Pattern by lazy {
        ConPattern(
                pat1 = TokenPattern(OpenRep),
                pat2 = ConPattern(
                        pat1 = RepPattern(pat.compile),
                        pat2 = TokenPattern(CloseRep)
                )
        )
    }
}

/**
 * A parser representing zero or one instances of another parser.
 */
data class OptParser<A>(private val pat: Parser<A>) : Parser<Option<A>>() {
    override val compile: Pattern by lazy {
        ConPattern(
                pat1 = TokenPattern(OpenOpt),
                pat2 = ConPattern(
                        pat1 = OptPattern(pat.compile),
                        pat2 = TokenPattern(CloseOpt)
                )
        )
    }
}

private object Reduction {
    private var id = 0

    val nextId: Int
        get() {
            id += 1
            return id
        }
}

/**
 * A parser which converts the parse tree of one parse into a new parse tree.
 */
class RedParser<A, B>(private val pat: Parser<A>,
                      f: (A) -> B) : Parser<B>() {
    @Suppress("UNCHECKED_CAST")
    private val g: (Any) -> Any = f as (Any) -> Any
    private val id by lazy { Reduction.nextId }

    override val compile: Pattern by lazy {
        ConPattern(
                pat1 = TokenPattern(OpenRed(g, id)),
                pat2 = ConPattern(
                        pat1 = pat.compile,
                        pat2 = TokenPattern(CloseRed(g, id))
                )
        )
    }
}

