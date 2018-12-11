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

interface exp
interface atom : exp

data class err(val msg: String?) : atom {
    override fun toString(): String = ":error\n${msg ?: "unknown error"}"
}

object unit : atom {
    override fun toString(): String = ":unit"
}

data class _list(private val value: exps) : exp, exps by value

sealed class number<T : Number>(open val value: T) : atom {
    open val asDouble: Double by lazy { value.toDouble() }
    override fun toString(): String = ":number(${value::class.simpleName}) $value"
}

sealed class integer<T : Number>(override val value: T) : number<T>(value) {
    val asLong: Long by lazy { value.toLong() }
    override fun toString(): String = ":integer(${value::class.simpleName}) $value"
}

sealed class decimal<T : Number>(override val value: T) : number<T>(value) {
    override val asDouble: Double by lazy { value.toDouble() }
    override fun toString(): String = ":decimal(${value::class.simpleName}) $value"
}

sealed class collection(protected open val value: Collection<exp>) : atom, Collection<exp> by value {
    override fun toString(): String = ":collection(${value::class.simpleName}) $value"
}

data class keyword(val value: String) : atom {
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
    override fun toString(): String = ":map ${value.map { (k, v) -> "$k -> $v" }}"
}

data class func(private val func: (exps) -> exp) : exp, ((exps) -> exp) by func {
    override fun toString(): String = ":func"
}

data class char(val value: Char) : atom {
    override fun toString(): String = ":char $value"
}

data class string(val value: String) : atom {
    override fun toString(): String = ":string $value"
}

data class symbol(val value: String) : atom {
    override fun toString(): String = ":symbol $value"
}

data class bool(val value: Boolean) : atom {
    override fun toString(): String = ":bool $value"
}

data class byte(override val value: Byte) : integer<Byte>(value) {
    override fun toString(): String = ":byte $value"
}

data class short(override val value: Short) : integer<Short>(value) {
    override fun toString(): String = ":short $value"
}

data class int(override val value: Int) : integer<Int>(value) {
    override fun toString(): String = ":int $value"
}

data class long(override val value: Long) : integer<Long>(value) {
    override fun toString(): String = ":long $value"
}

data class float(override val value: Float) : decimal<Float>(value) {
    override fun toString(): String = ":float $value"
}

data class double(override val value: Double) : decimal<Double>(value) {
    override fun toString(): String = ":double $value"
}

