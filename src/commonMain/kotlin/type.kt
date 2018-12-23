@file:Suppress("ClassName")

package klisp

typealias env = MutableMap<symbol, exp>
typealias exps = List<exp>
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
    MAP,
    QUOTE;

    companion object {
        private val aliases = values().mapNotNull { it.aliases }.flatten()
        private val values = specialForm.values().map { it.name.toLowerCase() }
        fun isSpecial(s: String): Boolean = (values + aliases).any { it == s }
        fun isSpecial(s: symbol): Boolean = isSpecial(s.value)
        fun fromString(s: String): specialForm {
            for (v in values()) {
                if (v.aliases?.contains(s) == true)
                    return v
            }
            return specialForm.valueOf(s.toUpperCase())
        }

        fun fromSymbol(s: symbol): specialForm = specialForm.fromString(s.value)
    }
}

@ExperimentalUnsignedTypes
enum class type(val constructor: ((s: String) -> exp)? = null,
                val isNumber: Boolean = true) {
    _bool({ bool.fromString(it) }),
    _byte({ byte.fromString(it) }),
    _ubyte({ ubyte.fromString(it) }),
    _short({ short.fromString(it) }),
    _ushort({ ushort.fromString(it) }),
    _int({ int.fromString(it) }),
    _uint({ uint.fromString(it) }),
    _long({ long.fromString(it) }),
    _ulong({ ulong.fromString(it) }),
    _float({ float.fromString(it) }),
    _double({ double.fromString(it) }),
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
    fun serialize(): String
    fun deserialize(s: String): atom
}

data class err(val msg: String?) : atom, serializable {
    override fun serialize(): String = (msg ?: "null").quoted
    override fun deserialize(s: String): err = err(s)
    override var meta: exp? = null
    override fun toString(): String = ":error\n${msg ?: "unknown error"}"
}

object nil : atom, serializable {
    override fun serialize(): String = "nil".quoted
    override fun deserialize(s: String): nil = this
    override var meta: exp? = null
    override fun toString(): String = ":nil"
}

object unit : atom {
    override fun serialize(): String = "unit".quoted
    override fun deserialize(s: String): atom = this
    override var meta: exp? = null
    override fun toString(): String = ":unit"
}

data class _list(private val value: exps) : exp, exps by value {
    override fun serialize(): String = throw NotImplementedError("_list is internal, this should not happen")
    override fun deserialize(s: String): atom = throw NotImplementedError("_list is internal, this should not happen")
    override var meta: exp? = null
    override fun toString(): String = "$value"
}

sealed class scalar : atom, serializable {
    override var meta: exp? = null
}

sealed class number<T : Number>(val numericValue: T) : scalar(), serializable {
    override fun serialize(): String = numericValue.toString()
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
    override fun serialize(): String = "[${value.joinToString(",", transform = exp::serialize)}]"
    override fun deserialize(s: String): atom = s.drop(1).dropLast(1).split(",").map { parseAtom(it) } as atom
    override var meta: exp? = null
    override fun toString(): String = ":collection $value"
}

data class keyword(val value: String) : atom, serializable {
    override fun serialize(): String = value.substringAfter(':').quoted
    override fun deserialize(s: String): keyword = keyword(":$s")
    override var meta: exp? = null
    override fun toString(): String = ":keyword ${value.substring(1)}"
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
    override fun serialize(): String = "{${value.map { (k, v) -> "${k.serialize()}:${v.serialize()}" }.joinToString(",")}}"
    override fun deserialize(s: String): map = throw NotImplementedError()
    override var meta: exp? = null
    override fun toString(): String = ":map ${value.map { (k, v) -> "$k -> $v" }}"
}

data class func(private val func: (exps) -> exp) : exp, ((exps) -> exp) by func {
    companion object {
        private fun parseMeta(m: exp?) = if (m !== null && DEBUG) "<$m>" else ""
    }

    override fun serialize(): String = throw IllegalStateException("can't serialize a function")
    override fun deserialize(s: String): atom = throw IllegalStateException("can't deserialize a function")
    override var meta: exp? = null
    override fun toString(): String = ":func ${parseMeta(meta)}"

    override fun invoke(p1: exps): exp {
        val _start = if (PROFILE) Platform.getTimeNanos() else 0
        val res = func(p1)
        when {
            PROFILE && DEBUG -> LOGGER.trace(":func ${parseMeta(meta)} ${took(_start)}")
            DEBUG -> LOGGER.debug(":func ${parseMeta(meta)}")
            PROFILE -> LOGGER.trace(":func ${took(_start)}")
        }
        return res
    }
}

data class char(val value: Char) : scalar() {
    override fun serialize(): String = value.toQuotedString()
    override fun deserialize(s: String): atom = char(s.first())
    override fun toString(): String = ":char $value"
}

data class string(val value: String) : scalar() {
    override fun serialize(): String = value
    override fun deserialize(s: String): atom = string(s)
    override fun toString(): String = ":string $value"
}

data class symbol(val value: String) : scalar() {
    override fun serialize(): String = value
    override fun deserialize(s: String): atom = symbol(s)
    override fun toString(): String = ":symbol $value"
}

data class bool(val value: Boolean) : integer<Byte>(if (value) 1 else 0) {
    companion object {
        fun fromString(s: String): exp = tryOrNil {
            if (s.toBoolean()) bool(true) else nil
        }
    }

    override fun serialize(): String = value.toString()
    override fun deserialize(s: String): atom = bool(s.toBoolean())
    override fun toString(): String = ":bool $value"
}

data class byte(val value: Byte) : integer<Byte>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { byte(s.toByte()) }
    }

    override fun deserialize(s: String): atom = byte(s.toByte())
    override fun toString(): String = ":byte $value"
}

data class short(val value: Short) : integer<Short>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { short(s.toShort()) }
    }

    override fun deserialize(s: String): atom = short(s.toShort())
    override fun toString(): String = ":short $value"
}

data class int(val value: Int) : integer<Int>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { int(s.toInt()) }
    }

    override fun deserialize(s: String): atom = int(s.toInt())
    override fun toString(): String = ":int $value"
}

data class long(val value: Long) : integer<Long>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { long(s.toLong()) }
    }

    override fun deserialize(s: String): atom = long(s.toLong())
    override fun toString(): String = ":long $value"
}

data class float(val value: Float) : decimal<Float>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil {
            val f = s.toFloat()
            if (f.isFinite()) float(f)
            else nil
        }
    }

    override fun deserialize(s: String): atom = float(s.toFloat())
    override fun toString(): String = ":float $value"
}

data class double(val value: Double) : decimal<Double>(value) {
    companion object {
        fun fromString(s: String): exp = tryOrNil {
            val d = s.toDouble()
            if (d.isFinite()) double(d)
            else nil
        }
    }

    override fun deserialize(s: String): atom = double(s.toDouble())
    override fun toString(): String = ":double $value"
}

@ExperimentalUnsignedTypes
data class ubyte(val value: UByte) : integer<Short>(value.toShort()) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { ubyte(s.toUByte()) }
    }

    override fun deserialize(s: String): atom = ubyte(s.toUByte())
    override fun toString(): String = ":ubyte $value"
}

@ExperimentalUnsignedTypes
data class ushort(val value: UShort) : integer<Int>(value.toInt()) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { ushort(s.toUShort()) }
    }

    override fun deserialize(s: String): atom = ushort(s.toUShort())
    override fun toString(): String = ":ushort $value"
}

@ExperimentalUnsignedTypes
data class uint(val value: UInt) : integer<Long>(value.toLong()) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { uint(s.toUInt()) }
    }

    override fun deserialize(s: String): atom = uint(s.toUInt())
    override fun toString(): String = ":uint $value"
}

@ExperimentalUnsignedTypes
data class ulong(val value: ULong) : decimal<Double>(value.toString().toDouble()) {
    companion object {
        fun fromString(s: String): exp = tryOrNil { ulong(s.toULong()) }
    }

    override fun deserialize(s: String): atom = ulong(s.toULong())
    override fun toString(): String = ":ulong $value"
}
