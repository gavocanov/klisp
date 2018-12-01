package klisp

fun begin(args: List<exp>): exp =
        args.last()

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

fun lte(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> bool(n1.value <= n2.value)
        n1 is float && n2 is float -> bool(n1.value <= n2.value)
        n1 is float && n2 is int -> bool(n1.value <= n2.value)
        n2 is float && n1 is int -> bool(n1.value <= n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation <=")
    }
}

fun lt(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> bool(n1.value < n2.value)
        n1 is float && n2 is float -> bool(n1.value < n2.value)
        n1 is float && n2 is int -> bool(n1.value < n2.value)
        n2 is float && n1 is int -> bool(n1.value < n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation <")
    }
}

fun gte(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> bool(n1.value >= n2.value)
        n1 is float && n2 is float -> bool(n1.value >= n2.value)
        n1 is float && n2 is int -> bool(n1.value >= n2.value)
        n2 is float && n1 is int -> bool(n1.value >= n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation >=")
    }
}

fun gt(args: List<exp>): exp {
    val (n1, n2) = args
    return when {
        n1 is int && n2 is int -> bool(n1.value > n2.value)
        n1 is float && n2 is float -> bool(n1.value > n2.value)
        n1 is float && n2 is int -> bool(n1.value > n2.value)
        n2 is float && n1 is int -> bool(n1.value > n2.value)
        else -> throw IllegalStateException("invalid operands ($n1, $n2) for operation >")
    }
}

fun isString(args: List<exp>): exp {
    if (args.size != 1) throw IllegalArgumentException("string? must have 1 parameter, got ${args.size}")
    val s = args.first()
    return bool(try {
        s as symbol
        true
    } catch (_: Throwable) {
        false
    })
}

fun isa(args: List<exp>): exp {
    if (args.size != 2) throw IllegalArgumentException("is? must have 2 parameters, got ${args.size}")
    val (f, s) = args
    return bool(f === s)
}

fun eq(args: List<exp>): exp {
    if (args.size != 2) throw IllegalArgumentException("eq? must have 2 parameters, got ${args.size}")
    val (f, s) = args
    return bool(f == s)
}

