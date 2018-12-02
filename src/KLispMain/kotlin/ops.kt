package klisp

fun begin(args: List<exp>): exp {
    require(args.size > 1) {"begin should have at least 1 argument"}
    return args.last()
}

fun div(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> int(n1.value / n2.value)
        n1 is float && n2 is float -> float(n1.value / n2.value)
        n1 is float && n2 is int -> float(n1.value / n2.value)
        n2 is float && n1 is int -> float(n1.value / n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation /")
    }
}

fun minus(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> int(n1.value - n2.value)
        n1 is float && n2 is float -> float(n1.value - n2.value)
        n1 is float && n2 is int -> float(n1.value - n2.value)
        n2 is float && n1 is int -> float(n1.value - n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation -")
    }
}

fun plus(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> int(n1.value + n2.value)
        n1 is float && n2 is float -> float(n1.value + n2.value)
        n1 is float && n2 is int -> float(n1.value + n2.value)
        n2 is float && n1 is int -> float(n1.value + n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation +")
    }
}

fun mul(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> int(n1.value * n2.value)
        n1 is float && n2 is float -> float(n1.value * n2.value)
        n1 is float && n2 is int -> float(n1.value * n2.value)
        n2 is float && n1 is int -> float(n1.value * n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation *")
    }
}

fun compare(op: CompareOps, args: List<exp>): Boolean {
    require(args.size == 2) { "$op should have 2 arguments, got ${args.size}" }
    val (x, y) = args
    require(args.all { it is number<*> }) { "$op should have numeric arguments" }
    x as number<*>
    y as number<*>
    return when (op) {
        CompareOps.lte -> x.asDouble <= y.asDouble
        CompareOps.lt -> x.asDouble < y.asDouble
        CompareOps.gte -> x.asDouble >= y.asDouble
        CompareOps.gt -> x.asDouble > y.asDouble
        CompareOps.eq -> x.asDouble == y.asDouble
    }
}

fun isa(type: Types, args: List<exp>): exp {
    require(args.size == 1) { "$type? must have 1 argument, got ${args.size}" }
    val value = args.first()
    return bool(
            try {
                when (type) {
                    Types.byte -> value as byte
                    Types.short -> value as short
                    Types.int -> value as int
                    Types.long -> value as long
                    Types.float -> value as float
                    Types.double -> value as double
                    Types.char -> value as char
                    Types.string -> value as string
                    Types.bool -> value as bool
                }
                true
            } catch (_: Throwable) {
                false
            }
    )
}

fun _is(args: List<exp>): exp {
    require(args.size == 2) { "is? must have 2 arguments, got ${args.size}" }
    val (f, s) = args
    return bool(f === s)
}

fun eq(args: List<exp>): exp {
    require(args.size == 2) { "eq? must have 2 arguments, got ${args.size}" }
    val (f, s) = args
    return bool(f == s)
}

