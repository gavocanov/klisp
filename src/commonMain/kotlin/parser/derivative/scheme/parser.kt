package klisp.parser.derivative.scheme

import klisp.Memoize
import klisp.None
import klisp.Some
import klisp.cons
import klisp.parser.derivative.BooleanToken
import klisp.parser.derivative.GenericParser
import klisp.parser.derivative.IntToken
import klisp.parser.derivative.LiveStream
import klisp.parser.derivative.LiveStreamSource
import klisp.parser.derivative.ParsingMachine
import klisp.parser.derivative.PunctToken
import klisp.parser.derivative.StringToken
import klisp.parser.derivative.SymbolToken
import klisp.parser.derivative.Token
import klisp.parser.derivative.TokenParser
import klisp.unApply

object SXParser {
    val SX = GenericParser<SExp>()
    private val SXList = GenericParser<List<SExp>>()
    private val Char.P: TokenParser get() = TokenParser(this.toString())

    init {
        // @formatter:off
        SX `ːː=` ('('.P `~` SXList `~` (('.'.P `~` SX).`?`) `~` ')'.P `==▷` { (f, _) ->
            val s = f.second
            val l = f.first.second
            when(s) {
                is None -> SExp.apply(l)
                is Some -> SExp.apply(l, s().second)
            }
        })

        SX `ːː=` (TokenParser(SymbolToken.tag)  `==▷` { SName.from((it as SymbolToken).value) })
        SX `ːː=` (TokenParser(IntToken.tag)     `==▷` { SInt((it as IntToken).value) })
        SX `ːː=` (TokenParser(StringToken.tag)  `==▷` { SText((it as StringToken).value) })
        SX `ːː=` (TokenParser(BooleanToken.tag) `==▷` { SBoolean((it as BooleanToken).value) })

        SXList `ːː=` (SX.`*`)
        // @formatter:on
    }
}

abstract class SExp {
    @Suppress("UNCHECKED_CAST")
    companion object {
        var shouldNamesBeSymbols = true

        infix fun apply(list: List<SExp?>): SExp {
            val (h, t) = list unApply 2
            return if (h is Some<*> && t is Some<*>) SConcat(h() as SExp, apply(t() as List<SExp?>))
            else SNil()
        }

        fun apply(list: List<SExp?>, tombstone: SExp): SExp {
            val (h, t) = list unApply 2
            return if (h is Some<*> && t is Some<*>)
                SConcat(h() as SExp, apply(t() as List<SExp?>, tombstone))
            else
                tombstone
        }

        private var maxSerialNumber = 0L
        fun allocateSerialNumber(): Long {
            maxSerialNumber += 1
            return maxSerialNumber
        }
    }

    val serialNumber: Long by lazy { SExp.allocateSerialNumber() }

    abstract override fun toString(): String
    abstract fun toDottedList(): Pair<List<SExp>, SExp>

    open fun toList(): List<SExp> = throw IllegalStateException("cannot convert ${this::class} to list")
    open val isKeyword: Boolean = false
    open val isInteger: Boolean = false
    open val isList: Boolean = false
    open val isPair: Boolean = false
    open val isNull: Boolean = false
    open val isSymbol: Boolean = false
    open val isName: Boolean = false
    open val isChar: Boolean = false
    open val isString: Boolean = false
    open val isBoolean: Boolean = false
}

class SNil : SExp() {
    override fun toString(): String = "()"
    override fun toList(): List<SExp> = emptyList()
    override fun toDottedList(): Pair<List<Nothing>, SNil> = emptyList<Nothing>() to this
    override val isList: Boolean = true
    override val isNull: Boolean = true
}

data class SInt(val value: Int) : SExp() {
    override fun toString(): String = "$value"
    override fun toDottedList(): Pair<List<Nothing>, SInt> = emptyList<Nothing>() to this
    override val isInteger: Boolean = true
}

@Suppress("unused")
data class SChar(val value: Char) : SExp() {
    override fun toString(): String = "$value"
    override fun toDottedList(): Pair<List<Nothing>, SChar> = emptyList<Nothing>() to this
    override val isChar: Boolean = true
}

data class SText(val value: String) : SExp() {
    override fun toString(): String = value
    override fun toDottedList(): Pair<List<Nothing>, SText> = emptyList<Nothing>() to this
    override val isString: Boolean = true
}

data class SBoolean(val value: Boolean) : SExp() {
    override fun toString(): String = if (value) "#t" else "#f"
    override fun toDottedList(): Pair<List<Nothing>, SBoolean> = emptyList<Nothing>() to this
    override val isBoolean: Boolean = true
}

@Suppress("unused")
data class SKeyword(val value: String) : SExp(), Comparable<SKeyword> {
    override fun toString(): String = "#:$value"
    override fun toDottedList(): Pair<List<Nothing>, SKeyword> = emptyList<Nothing>() to this
    override val isKeyword: Boolean = true
    override fun compareTo(other: SKeyword): Int = value.compareTo(other.value)
}

abstract class SSymbol(val string: String) : SExp()
data class SName(val value: String, val version: Int) : SSymbol(value), Comparable<SName> {
    companion object {
        private val nameTable = Memoize<String, SName>(HashMap()) { SName(it, 0) }
        fun from(string: String): SName = nameTable(string)
    }

    override fun toString(): String = when (version) {
        0 -> string
        else -> {
            if (SExp.shouldNamesBeSymbols) "$value/$$version"
            else "#name[$string $version]"
        }
    }

    override fun toDottedList(): Pair<List<Nothing>, SName> = emptyList<Nothing>() to this
    override val isSymbol: Boolean = true
    override val isName: Boolean = true

    override fun compareTo(other: SName): Int {
        val (s2, v2) = other
        val cmpStr = value.compareTo(s2)
        return if (cmpStr != 0) cmpStr
        else version.compareTo(v2)
    }

    override fun hashCode(): Int = value.hashCode() * 10 + version
    override fun equals(other: Any?): Boolean {
        if (other !is SName) return false
        val (s2, v2) = other
        return value == s2 && version == v2
    }
}

data class SConcat(var car: SExp, var cdr: SExp) : SExp() {
    override fun toDottedList(): Pair<List<SExp>, SExp> {
        val (lst, end) = cdr.toDottedList()
        return car cons lst to end
    }

    override fun toString(): String {
        val (l, s) = toDottedList()
        return when (s) {
            is SNil -> "(${l.joinToString(" ")})"
            else -> "(${l.joinToString(" ")} . $s)"
        }
    }

    override fun toList(): List<SExp> = car cons cdr.toList()
    override val isPair: Boolean = true
    override val isList: Boolean = cdr.isList
}

fun test1() {
    val chars = LiveStream("(define   (inc   x)   (+\n\n x a sntaoheusah\n #! #! aoeuaoeu !# !# 1)) (inc 3)")
    println("chars: $chars")
    val lexer = SXLexer()
    lexer.lex(chars)
    println("tokens: " + lexer.output)
    println("parse:")
    val ast = SXParser.SX.parse(lexer.output)
    println(ast)
}

fun test2() {
    val S = SXParser.SX.compile
    val core = listOf(
            PunctToken("("),
//            PunctToken("("),
//            PunctToken(")"),
            PunctToken(")")
    )

    var input = core
/*    var rounds = 1
    while (rounds > 0) {
        input += input
        rounds -= 1
    }

    input = PunctToken("(") cons (input + listOf(PunctToken(")")))*/
    println("input: $input")
    println("input.length = ${input.size}")

    val source: LiveStreamSource<Token> = LiveStreamSource()
    source += input
    val stream = LiveStream(source)
    val pm = ParsingMachine(S, stream)

    println("parse string generated!")
    println(pm.output.head)
    println(pm.output.head.reduce())
    println(pm.output)
}

fun main(args: Array<String>) {
    test2()
}
