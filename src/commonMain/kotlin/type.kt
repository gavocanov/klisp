@file:Suppress("ClassName")

package klisp

typealias env = MutableMap<symbol, exp>
typealias exps = List<exp>
typealias kmap = Map<keyword, exp>

data class ExitException(val msg: String) : Throwable(msg)

enum class compareOp { lte, lt, gte, gt, eq }

enum class mathOp { plus, minus, div, mul }

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

enum class type {
    byte,
    short,
    int,
    long,
    float,
    double,
    char,
    string,
    keyword,
    bool,
    list,
    set,
    map,
    collection,
    number,
    integer,
    decimal,
    symbol,
    atom
}

interface exp {
    var meta: exp?
}

interface atom : exp {
    override var meta: exp?
}

data class err(val msg: String?) : atom {
    override var meta: exp? = null
    override fun toString(): String = ":error\n${msg ?: "unknown error"}"
}

object unit : atom {
    override var meta: exp? = null
    override fun toString(): String = ":unit"
}

data class _list(private val value: exps) : exp, exps by value {
    override var meta: exp? = null
    override fun toString(): String = "$value"
}

sealed class scalar : atom {
    override var meta: exp? = null
}

sealed class number<T : Number>(val numericValue: T) : scalar() {
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

sealed class collection(protected open val value: Collection<exp>) : atom, Collection<exp> by value {
    override var meta: exp? = null
    override fun toString(): String = ":collection(${value::class.simpleName}) $value"
}

data class keyword(val value: String) : atom {
    override var meta: exp? = null
    override fun toString(): String = ":keyword ${value.substring(1)}"
}

data class list(override val value: exps) : collection(value), exps by value {
    override val size: Int = value.size
    override fun contains(element: exp): Boolean = value.contains(element)
    override fun containsAll(elements: Collection<exp>): Boolean = value.containsAll(elements)
    override fun isEmpty(): Boolean = value.isEmpty()
    override fun iterator(): Iterator<exp> = value.iterator()
    override fun toString(): String = ":list $value"
    override operator fun get(index: Int): exp = value[index]
}

data class set(override val value: Set<exp>) : collection(value), Set<exp> by value {
    override val size: Int = value.size
    override fun contains(element: exp): Boolean = value.contains(element)
    override fun containsAll(elements: Collection<exp>): Boolean = value.containsAll(elements)
    override fun isEmpty(): Boolean = value.isEmpty()
    override fun iterator(): Iterator<exp> = value.iterator()
    override fun toString(): String = ":set $value"
    operator fun get(index: Int): exp = value.toList()[index]
}

data class map(private val value: kmap) : atom, kmap by value {
    override var meta: exp? = null
    override fun toString(): String = ":map ${value.map { (k, v) -> "$k -> $v" }}"
}

data class func(private val func: (exps) -> exp) : exp, ((exps) -> exp) by func {
    companion object {
        private fun parseMeta(m: exp?) = if (m !== null && DEBUG) "<$m>" else ""
    }

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
    override fun toString(): String = ":char $value"
}

data class string(val value: String) : scalar() {
    override fun toString(): String = ":string $value"
}

data class symbol(val value: String) : scalar() {
    override fun toString(): String = ":symbol $value"
}

data class bool(val value: Boolean) : integer<Byte>(if (value) 1 else 0) {
    override fun toString(): String = ":bool $value"
}

data class byte(val value: Byte) : integer<Byte>(value) {
    override fun toString(): String = ":byte $value"
}

data class short(val value: Short) : integer<Short>(value) {
    override fun toString(): String = ":short $value"
}

data class int(val value: Int) : integer<Int>(value) {
    override fun toString(): String = ":int $value"
}

data class long(val value: Long) : integer<Long>(value) {
    override fun toString(): String = ":long $value"
}

data class float(val value: Float) : decimal<Float>(value) {
    override fun toString(): String = ":float $value"
}

data class double(val value: Double) : decimal<Double>(value) {
    override fun toString(): String = ":double $value"
}

@ExperimentalUnsignedTypes
data class ubyte(val value: UByte) : integer<Short>(value.toShort()) {
    override fun toString(): String = ":ubyte $value"
}

@ExperimentalUnsignedTypes
data class ushort(val value: UShort) : integer<Int>(value.toInt()) {
    override fun toString(): String = ":ushort $value"
}

@ExperimentalUnsignedTypes
data class uint(val value: UInt) : integer<Long>(value.toLong()) {
    override fun toString(): String = ":uint $value"
}

@ExperimentalUnsignedTypes
data class ulong(val value: ULong) : integer<Long>(value.toLong()) {
    override fun toString(): String = ":ulong $value"
}

