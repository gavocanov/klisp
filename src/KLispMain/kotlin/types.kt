@file:Suppress("ClassName")

package klisp

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
    val asDouble: Double by lazy { value.toDouble() }
    override fun toString(): String {
        return ":number $value"
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

data class string(val value: String) : atom() {
    override fun toString(): String {
        return ":string $value"
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

data class byte(override val value: Byte) : number<Byte>(value) {
    override fun toString(): String {
        return ":byte $value"
    }
}

data class short(override val value: Short) : number<Short>(value) {
    override fun toString(): String {
        return ":short $value"
    }
}

data class int(override val value: Int) : number<Int>(value) {
    override fun toString(): String {
        return ":int $value"
    }
}

data class long(override val value: Long) : number<Long>(value) {
    override fun toString(): String {
        return ":long $value"
    }
}

data class float(override val value: Float) : number<Float>(value) {
    override fun toString(): String {
        return ":float $value"
    }
}

data class double(override val value: Double) : number<Double>(value) {
    override fun toString(): String {
        return ":double $value"
    }
}

enum class CompareOps { lte, lt, gte, gt, eq }
enum class Types { byte, short, int, long, float, double, char, string, bool }

