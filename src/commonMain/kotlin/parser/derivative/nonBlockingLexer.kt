@file:Suppress("unused", "FINAL_UPPER_BOUND")

package klisp.parser.derivative

import klisp.cons
import klisp.exIfNull
import klisp.first
import klisp.last
import klisp.rest
import klisp.reversed

/**
 * A non-blocking lexer consumes a live stream of characters (or objects
 * which can be converted into characters) and emits a stream of type-A
 * tokens.
 */
abstract class NonBlockingLexer<C : Char, A> {
    private lateinit var outputSource: LiveStreamSource<A>
    private lateinit var _output: LiveStream<A>

    /**
     * The output stream for this lexer.
     */
    val output get() = _output

    /**
     * A lexer rule represents a regular language to match and an action
     * to fire once matched.
     */
    protected inner class LexerRule(private val regex: RegularLanguage,
                                    private val action: (List<C>) -> LexerState) {

        val mustAccept get() = regex.isEmptyString
        val accepts get() = regex.acceptsEmptyString
        val rejects get() = regex.rejectsAll
        fun deriveEND() = LexerRule(regex.deriveEND(), action)
        infix fun derive(c: Char) = LexerRule(regex derive c, action)
        infix fun fire(chars: List<C>) = action(chars)
        override fun toString(): String = regex.toString()
    }

    /**
     * A lexer state represents the current state of the lexer.
     * Each state contains rules to match.
     */
    protected abstract inner class LexerState {
        /**
         * Rules for this lexing state.
         */
        protected abstract val rules: List<LexerRule>
        /**
         * Characters lexed so far in this state.
         */
        protected abstract val chars: List<C>

        /**
         * True if this state could accept.
         */
        val isAccept get() = rules.any { it.accepts }

        /**
         * True if this state accepts, but no successor possible could.
         */
        val mustAccept: Boolean
            get() {
                var sawMustAccept = false
                for (r in rules) {
                    if (r.mustAccept && !sawMustAccept)
                        sawMustAccept = true
                    else if (!r.rejects)
                        return false
                }
                return sawMustAccept
            }

        /**
         * True if no string can ever be matched from this state.
         */
        val isReject get() = rules.all { it.rejects }

        /**
         * Causes the characters lexed thus far to be accepted;
         * returns the next lexing state.
         */
        open fun fire(): LexerState {
            val accepting = rules.filter { it.accepts }
            return accepting.last.fire(chars.reversed)
        }

        /**
         * Checks to see if any of the rules match the end of input.
         * @return the lexer state after such a match.
         */
        fun terminate(): LexerState = MinorLexerState(
                chars = chars,
                rules = rules
                        .map { it.deriveEND() }
                        .filter { !it.rejects }
        )

        /**
         * Checks to see if any of the rules match the input c.
         * @return the lexer state after such a match.
         */
        infix fun next(c: C): LexerState = MinorLexerState(
                chars = c cons chars,
                rules = rules
                        .map { it derive c }
                        .filter { !it.rejects })
    }

    /**
     * A state that rejects everything.
     */
    private val RejectLexerState: NonBlockingLexer<C, A>.LexerState =
            object : NonBlockingLexer<C, A>.LexerState() {
                override fun fire(): NonBlockingLexer<C, A>.LexerState =
                        throw IllegalStateException("lexing failed right before: $currentInput")

                override val rules: List<NonBlockingLexer<C, A>.LexerRule> = emptyList()
                override val chars: List<C> = emptyList()
            }

    /**
    A minor lexer state is an intermediate lexing state.
     */
    protected inner class MinorLexerState(override val chars: List<C>,
                                          override val rules: List<LexerRule>) : LexerState()

    interface Matchable<C : Char> {
        infix fun apply(action: () -> Unit)
        infix fun over(action: (List<C>) -> Unit)
    }

    /**
     * Represents a half-defined match-and-switch rule, which needs the
     * action to be completed.
     */
    protected interface Switchable<C : Char, A> {
        /**
         * Switches to a state, ignoring the input.
         */
        infix fun to(action: () -> NonBlockingLexer<C, A>.LexerState)

        /**
         * Switches to a state, consuming the input.
         */
        infix fun over(action: (List<C>) -> NonBlockingLexer<C, A>.LexerState)
    }

    /**
     * A major lexing state is defined by the programmer.
     */
    protected open inner class MajorLexerState : NonBlockingLexer<C, A>.LexerState() {
        /**
         * Major states begin with an empty character list.
         */
        override val chars = emptyList<C>()

        private var _rules: List<LexerRule> = emptyList()

        /**
         * The rules for this state.
         */
        override val rules get() = _rules

        /**
         * Deletes all of the rules for this state.
         */
        fun reset() {
            _rules = emptyList()
        }

        /**
         * Adds a rule to this state which matches regex and fires action.
         */
        infix fun apply(regex: RegularLanguage): Matchable<C> = object : Matchable<C> {
            override infix fun apply(action: () -> Unit) {
                _rules = LexerRule(regex) {
                    action()
                    this@MajorLexerState
                } cons _rules
            }

            override infix fun over(action: (List<C>) -> Unit) {
                _rules = LexerRule(regex) { chars ->
                    action(chars)
                    this@MajorLexerState
                } cons _rules
            }
        }

        /**
         * Utility to reduce boilerplate
         */
        infix fun apply(s: String): Matchable<C> = apply(s.toRL())

        /**
         * Adds a rule to this state which matches regex, fires action and
         * switches to the lexer state returned by the action.
         */
        infix fun switchesOn(regex: RegularLanguage): Switchable<C, A> = object : Switchable<C, A> {
            override infix fun over(action: (List<C>) -> NonBlockingLexer<C, A>.LexerState) {
                _rules = LexerRule(regex, action) cons _rules
            }

            override infix fun to(action: () -> NonBlockingLexer<C, A>.LexerState) {
                _rules = LexerRule(regex) { action() } cons _rules
            }
        }

        /**
         * Utility to reduce boilerplate
         */
        infix fun switchesOn(s: String): Switchable<C, A> = switchesOn(s.toRL())
    }

    /**
     * A major lexer state which contains internal state.
     * Useful for lexing sequences like comments and strings.
     */
    protected open inner class StatefulMajorLexerState<S>(private var state: S) : MajorLexerState() {
        /**
         * @return the same lexer state, but with a new internal state.
         */
        infix fun apply(s: S): StatefulMajorLexerState<S> {
            state = s
            return this
        }

        /**
         * Adds a rule whose action also sees the current state.
         */
        fun update(regex: RegularLanguage, stateAction: (S, List<C>) -> LexerState) {
            val action = { chars: List<C> -> stateAction(this.state, chars) }
            super.switchesOn(regex) over action
        }

        /**
         * Utility to reduce boilerplate
         */
        fun update(s: String, stateAction: (S, List<C>) -> LexerState) =
                update(s.toRL(), stateAction)
    }

    /**
     * During lexing, the last encountered which accepted.
     */
    private lateinit var lastAcceptingState: LexerState

    /**
     * During lexing, the input associated with the last accepting state.
     */
    private var lastAcceptingInput: LiveStream<C>? = null

    /**
     * During lexing, the current lexer state.
     */
    private lateinit var currentState: LexerState

    /**
     * During lexing, the location in the current input.
     */
    private lateinit var currentInput: LiveStream<C>

    private val LiveStream<C>?.isNullOrEmpty: Boolean
        get() = if (this === null) true
        else this.isEmpty

    private val LiveStream<C>?.isPlugged: Boolean
        get() = if (this === null) false
        else this.isPlugged

    /**
     * Starts the lexer on the given input stream.
     * The field output will contain a live stream of the lexer output.
     */
    infix fun lex(input: LiveStream<C>) {
        currentState = MAIN
        currentInput = input
        outputSource = LiveStreamSource()
        _output = LiveStream(outputSource)
        input.source.addListener { work() }
        work()
    }

    /**
     * Forces the lexer to consume as much of the input as is available.
     */
    private fun work() {
        while (workStep()) {
        }
    }

    private fun workStep(): Boolean {
        // First, check to see if the current state must accept.
        if (currentState.mustAccept) {
            currentState = currentState.fire()
            lastAcceptingState = RejectLexerState
            lastAcceptingInput = null
            return true
        }

        // First, check to se if the current state accepts or rejects.
        if (currentState.isAccept) {
            lastAcceptingState = currentState
            lastAcceptingInput = currentInput
        } else if (currentState.isReject) {
            // Backtrack to the last accepting state; fail if none.
            currentState = lastAcceptingState.fire()
            currentInput = lastAcceptingInput.exIfNull
            lastAcceptingState = RejectLexerState
            lastAcceptingInput = null
            return true
        }

        // If at the end of the input, clean up:
        if (currentInput.isNullOrEmpty) {
            val terminalState = currentState.terminate()

            return if (terminalState.isAccept) {
                terminalState.fire()
                false
            } else {
                currentState = lastAcceptingState.fire()
                currentInput = lastAcceptingInput.exIfNull
                lastAcceptingState = RejectLexerState
                lastAcceptingInput = null
                true
            }
        }

        // If there's input left to process, process it:
        if (!currentInput.isPlugged) {
            val c = currentInput.head
            currentState = currentState.next(c)
            currentInput = currentInput.tail
        }

        // If more progress could be made, keep working.
        if (!currentInput.isPlugged || currentInput.isNullOrEmpty || currentState.isReject)
            return true

        // Check again to see if the current state must accept.
        if (currentState.mustAccept) {
            currentState = currentState.fire()
            lastAcceptingState = RejectLexerState
            lastAcceptingInput = null
            return true
        }

        return false
    }

    /**
     * Adds another token to the output.
     */
    protected infix fun emit(token: A) {
        outputSource += token
    }

    /**
     * Indicates that all input has been read.
     */
    protected fun terminate() {
        outputSource.terminate()
    }

    /**
     * @return a new lexing state.
     */
    protected fun State() = MajorLexerState()

    /**
     * @return a new lexing state with internal state.
     */
    protected fun <S> State(state: S) = StatefulMajorLexerState(state)

    /**
     * Initial state when lexing.
     */
    protected abstract val MAIN: LexerState

    /**
     * Char to regex utility
     */
    private fun Char.toRL(): RegularLanguage = Character(this)

    /**
     * String to regex utility
     */
    protected fun String.toRL(): RegularLanguage = when {
        this.length == 1 -> Character(first)
        this.isNotEmpty() -> Catenation(first.toRL(), rest.toRL())
        else -> ε
    }

    protected infix fun Char.thru(end: Char): RegularLanguage = CharSet((this..end).toSet())
}

