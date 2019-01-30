@file:Suppress("ClassName")

package klisp

import klisp.expected.Platform
import klisp.parser.parseStringAtom
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

typealias exps = List<exp>

typealias env = MutableMap<symbol, exp>

typealias kmap = Map<keyword, exp>

data class ExitException(val msg: String) : Throwable(msg)

enum class compareOp { lte, lt, gte, gt, eq }

enum class mathOp { plus, minus, div, mul, pow, rem, abs }

enum class specialForm(val aliases: List<String>? = null) {
    // symbols
    DEBUG,
    PROFILE,
    ENV(listOf("ls")),
    // expressions
    DEF(listOf("define")),
    LAMBDA(listOf("lam")),
    IF,
    UNLESS,
    WHEN,
    FMAP,
    FILTER,
    REDUCE,
    QUOTE;

    companion object {
        private val aliases = values().mapNotNull { it.aliases }.flatten()
        private val values = specialForm.values().map { it.name.toLowerCase() }
        fun isSpecial(s: String): Boolean = (values + aliases).any { it == s }
        fun isSpecial(s: symbol): Boolean = isSpecial(s.value)
        fun from(s: String): specialForm {
            for (v in values()) {
                if (v.aliases?.contains(s) == true)
                    return v
            }
            return specialForm.valueOf(s.toUpperCase())
        }

        fun fromSymbol(s: symbol): specialForm = specialForm.from(s.value)
    }
}

@ExperimentalUnsignedTypes
enum class type(val constructor: ((s: String) -> exp)? = null,
                val isNumber: Boolean = true) {
    _bool({ bool.from(it) }),
    _byte({ byte.from(it) }),
    _ubyte({ ubyte.from(it) }),
    _short({ short.from(it) }),
    _ushort({ ushort.from(it) }),
    _int({ int.from(it) }),
    _uint({ uint.from(it) }),
    _long({ long.from(it) }),
    _ulong({ ulong.from(it) }),
    _float({ float.from(it) }),
    _double({ double.from(it) }),
    _integer(null, false),
    _decimal(null, false),
    _number(null, false),
    _char(null, false),
    _string(null, false),
    _keyword(null, false),
    _list(null, false),
    _set(null, false),
    _map(null, false),
    _collection(null, false),
    _symbol(null, false),
    _atom(null, false)
}

interface exp : serializable {
    var meta: exp?
}

interface atom : exp {
    override var meta: exp?
}

interface serializable {
    fun toJson(): String
    fun fromJson(s: String): atom
}

data class err(val msg: String?) : atom, serializable {
    override fun toJson(): String = Json.stringify(String.serializer(), msg ?: "no message")
    override fun fromJson(s: String): err = err(s)
    override var meta: exp? = null
    override fun toString(): String = ":error\n${msg ?: "unknown error"}"
}

object nil : atom, serializable {
    override fun toJson(): String = Json.stringify(String.serializer(), "nil")
    override fun fromJson(s: String): nil = this
    override var meta: exp? = null
    override fun toString(): String = ":nil"
}

object unit : atom, serializable {
    override fun toJson(): String = Json.stringify(String.serializer(), "unit")
    override fun fromJson(s: String): atom = this
    override var meta: exp? = null
    override fun toString(): String = ":unit"
}

data class _list(private val value: exps) : exp, exps by value {
    override fun toJson(): String = throw NotImplementedError("_list is internal, this should not happen")
    override fun fromJson(s: String): atom = throw NotImplementedError("_list is internal, this should not happen")
    override var meta: exp? = null
    override fun toString(): String = "$value"
}

sealed class scalar : atom, serializable {
    override var meta: exp? = null
}

sealed class number<out T : Number>(val numericValue: T) : scalar(), serializable {
    open val asDouble: Double by lazy { numericValue.toDouble() }
    override fun toString(): String = ":number(${numericValue::class.simpleName}) $numericValue"
}

sealed class integer<T : Number>(val integerValue: T) : number<T>(integerValue) {
    val asLong: Long by lazy { integerValue.toLong() }
    override fun toString(): String = ":integer(${integerValue::class.simpleName}) $integerValue"
}

sealed class decimal<T : Number>(private val decimalValue: T) : number<T>(decimalValue) {
    override val asDouble: Double by lazy { decimalValue.toDouble() }
    override fun toString(): String = ":decimal(${decimalValue::class.simpleName}) $decimalValue"
}

@ExperimentalUnsignedTypes
sealed class collection(protected open val value: Collection<exp>) : atom, serializable, Collection<exp> by value {
    override fun toJson(): String = "[${value.joinToString(",", transform = exp::toJson)}]"
    override fun fromJson(s: String): atom = s.drop(1).dropLast(1).split(",").map { parseStringAtom(it) } as atom
    override var meta: exp? = null
    override fun toString(): String = ":collection $value"
}

@ExperimentalUnsignedTypes
data class list(override val value: exps) : collection(value), exps by value {
    override val size: Int = value.size
    override fun contains(element: exp): Boolean = value.contains(element)
    override fun containsAll(elements: Collection<exp>): Boolean = value.containsAll(elements)
    override fun isEmpty(): Boolean = value.isEmpty()
    override fun iterator(): Iterator<exp> = value.iterator()
    override fun toString(): String = ":list $value"
    override operator fun get(index: Int): exp = value[index]
}

@ExperimentalUnsignedTypes
data class set(override val value: Set<exp>) : collection(value), Set<exp> by value {
    override val size: Int = value.size
    override fun contains(element: exp): Boolean = value.contains(element)
    override fun containsAll(elements: Collection<exp>): Boolean = value.containsAll(elements)
    override fun isEmpty(): Boolean = value.isEmpty()
    override fun iterator(): Iterator<exp> = value.iterator()
    override fun toString(): String = ":set $value"
    operator fun get(index: Int): exp = value.toList()[index]
}

data class map(private val value: kmap) : atom, kmap by value, serializable {
    override fun toJson(): String = "{${value.map { (k, v) -> "${k.toJson()}:${v.toJson()}" }.joinToString(",")}}"
    override fun fromJson(s: String): map = throw NotImplementedError()
    override var meta: exp? = null
    override fun toString(): String = ":map ${value.map { (k, v) -> "$k -> $v" }}"
}

data class func(private val func: (exps) -> exp) : exp, ((exps) -> exp) by func {
    companion object {
        private fun parseMeta(m: exp?) = if (m !== null && DEBUG) "<$m>" else ""
    }

    override fun toJson(): String = throw IllegalStateException("can't serialize a function")
    override fun fromJson(s: String): atom = throw IllegalStateException("can't deserialize a function")
    override var meta: exp? = null
    override fun toString(): String = ":func ${parseMeta(meta)}"

    override fun invoke(p1: exps): exp {
        val _start = if (PROFILE) Platform.getTimeNanos() else 0
        val res = func(p1)
        when {
            PROFILE && DEBUG -> LOGGER.trace(":func ($p1) -> $res, ${took(_start)}")
            DEBUG -> LOGGER.debug(":func ($p1) -> $res")
            PROFILE -> LOGGER.trace(":func ${took(_start)}")
        }
        return res
    }
}

data class char(val value: Char) : scalar() {
    override fun toJson(): String = Json.stringify(String.serializer(), value.toString())
    override fun fromJson(s: String): atom = char(s.first())
    override fun toString(): String = ":char $value"
}

data class string(val value: String) : scalar() {
    override fun toJson(): String = Json.stringify(String.serializer(), value)
    override fun fromJson(s: String): atom = string(s)
    override fun toString(): String = ":string $value"
}

data class symbol(val value: String) : scalar() {
    override fun toJson(): String = Json.stringify(String.serializer(), value)
    override fun fromJson(s: String): atom = symbol(s)
    override fun toString(): String = ":symbol $value"
}

data class keyword(val value: String) : atom, serializable {
    override fun toJson(): String = Json.stringify(String.serializer(), value.substringAfter(':'))
    override fun fromJson(s: String): keyword = keyword(":$s")
    override var meta: exp? = null
    override fun toString(): String = ":keyword $value"
}

data class bool(val value: Boolean) : integer<Byte>(if (value) 1 else 0) {
    companion object {
        fun from(s: String): exp = when {
            s.toLowerCase() == "true" -> bool(true)
            s.toLowerCase() == "false" -> bool(false)
            else -> nil
        }
    }

    override fun toJson(): String = Json.stringify(Boolean.serializer(), value)
    override fun fromJson(s: String): atom = bool(s.toBoolean())
    override fun toString(): String = ":bool $value"
}

data class byte(val value: Byte) : integer<Byte>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil { byte(s.toByte()) }
    }

    override fun toJson(): String = Json.stringify(Byte.serializer(), value)
    override fun fromJson(s: String): atom = byte(s.toByte())
    override fun toString(): String = ":byte $value"
}

data class short(val value: Short) : integer<Short>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil { short(s.toShort()) }
    }

    override fun toJson(): String = Json.stringify(Short.serializer(), value)
    override fun fromJson(s: String): atom = short(s.toShort())
    override fun toString(): String = ":short $value"
}

data class int(val value: Int) : integer<Int>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil { int(s.toInt()) }
    }

    override fun toJson(): String = Json.stringify(Int.serializer(), value)
    override fun fromJson(s: String): atom = int(s.toInt())
    override fun toString(): String = ":int $value"
}

data class long(val value: Long) : integer<Long>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil { long(s.toLong()) }
    }

    override fun toJson(): String = Json.stringify(Long.serializer(), value)
    override fun fromJson(s: String): atom = long(s.toLong())
    override fun toString(): String = ":long $value"
}

data class float(val value: Float) : decimal<Float>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil {
            val f = s.toFloat()
            if (f == 0.0f && s.toDouble() != 0.0) // this happens, wtf?
                nil
            else {
                if (f.isFinite()) float(f)
                else nil
            }
        }
    }

    override fun toJson(): String = Json.stringify(Float.serializer(), value)
    override fun fromJson(s: String): atom = float(s.toFloat())
    override fun toString(): String = ":float $value"
}

@ExperimentalUnsignedTypes
data class double(val value: Double) : decimal<Double>(value) {
    companion object {
        fun from(s: String): exp = tryOrNil {
            val d = s.toDouble()
            if (d.isFinite()) double(d)
            else nil
        }

        fun from(num: Number) = double.from("$num")
        fun from(num: ULong) = double.from("$num")
    }

    override fun toJson(): String = Json.stringify(Double.serializer(), value)
    override fun fromJson(s: String): atom = double(s.toDouble())
    override fun toString(): String = ":double $value"
}

@ExperimentalUnsignedTypes
data class ubyte(val value: UByte) : integer<Short>(value.toShort()) {
    companion object {
        fun from(s: String): exp = tryOrNil { ubyte(s.toUByte()) }
    }

    override fun toJson(): String = Json.stringify(Short.serializer(), value.toShort())
    override fun fromJson(s: String): atom = ubyte(s.toUByte())
    override fun toString(): String = ":ubyte $value"
}

@ExperimentalUnsignedTypes
data class ushort(val value: UShort) : integer<Int>(value.toInt()) {
    companion object {
        fun from(s: String): exp = tryOrNil { ushort(s.toUShort()) }
    }

    override fun toJson(): String = Json.stringify(Int.serializer(), value.toInt())
    override fun fromJson(s: String): atom = ushort(s.toUShort())
    override fun toString(): String = ":ushort $value"
}

@ExperimentalUnsignedTypes
data class uint(val value: UInt) : integer<Long>(value.toLong()) {
    companion object {
        fun from(s: String): exp = tryOrNil { uint(s.toUInt()) }
    }

    override fun toJson(): String = Json.stringify(Long.serializer(), value.toLong())
    override fun fromJson(s: String): atom = uint(s.toUInt())
    override fun toString(): String = ":uint $value"
}

@ExperimentalUnsignedTypes
data class ulong(val value: ULong) : decimal<Double>(value.toString().toDouble()) {
    companion object {
        fun from(s: String): exp = tryOrNil { ulong(s.toULong()) }
        fun from(number: Number) = ulong(number.toString().toULong())
    }

    override fun toJson(): String = Json.stringify(Float.serializer(), value.toString().toFloat())
    override fun fromJson(s: String): atom = ulong(s.toULong())
    override fun toString(): String = ":ulong $value"
}
