@file:Suppress("ClassName")

package klisp

typealias env = MutableMap<symbol, exp>
typealias exps = List<exp>

sealed class exp {
    override fun toString(): String {
        val s = super.toString()
        return ":exp $s"
    }
}

sealed class atom : exp() {
    override fun toString(): String {
        val s = super.toString()
        return ":atom $s"
    }
}

sealed class number<T : Number>(open val value: T) : atom() {
    open val asDouble: Double by lazy { value.toDouble() }
    override fun toString(): String {
        return ":number $value"
    }
}

sealed class integer<T : Number>(override val value: T) : number<T>(value) {
    val asLong: Long by lazy { value.toLong() }
    override fun toString(): String {
        return ":integer $value"
    }
}

sealed class decimal<T : Number>(override val value: T) : number<T>(value) {
    override val asDouble: Double by lazy { value.toDouble() }
    override fun toString(): String {
        return ":decimal $value"
    }
}

object unit : atom() {
    override fun toString(): String {
        return ":unit"
    }
}

data class func(val func: (List<exp>) -> exp) : exp() {
    override fun toString(): String {
        return ":func"
    }
}

data class char(val value: Char) : atom() {
    override fun toString(): String {
        return ":char $value"
    }
}

data class symbol(val value: String) : atom() {
    override fun toString(): String {
        return ":symbol $value"
    }
}

data class bool(val value: Boolean) : atom() {
    override fun toString(): String {
        return ":bool $value"
    }
}

data class list(val value: List<exp>) : exp() {
    override fun toString(): String {
        return ":list $value"
    }
}

data class byte(override val value: Byte) : integer<Byte>(value) {
    override fun toString(): String {
        return ":byte $value"
    }
}

data class short(override val value: Short) : integer<Short>(value) {
    override fun toString(): String {
        return ":short $value"
    }
}

data class int(override val value: Int) : integer<Int>(value) {
    override fun toString(): String {
        return ":int $value"
    }
}

data class long(override val value: Long) : integer<Long>(value) {
    override fun toString(): String {
        return ":long $value"
    }
}

data class float(override val value: Float) : decimal<Float>(value) {
    override fun toString(): String {
        return ":float $value"
    }
}

data class double(override val value: Double) : decimal<Double>(value) {
    override fun toString(): String {
        return ":double $value"
    }
}

enum class compareOp { lte, lt, gte, gt, eq }
enum class type { byte, short, int, long, float, double, char, string, bool }
enum class mathOp { plus, minus, div, mul }
