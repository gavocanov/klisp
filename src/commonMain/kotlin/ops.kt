package klisp

fun begin(args: exps): exp {
    require(args.size > 1) { "begin should have at least 1 argument" }
    return args.last()
}

fun foldableMathOp(op: mathOp, args: exps): exp {
    require(args.size >= 2) { "$op expects at least 2 arguments" }
    require(args.all { it is number<*> }) { "$op expects all numeric arguments" }
    return when {
        args.all { it is byte } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as byte).value })
                mathOp.minus -> long(args.fold(0L) { a, n -> a - (n as byte).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as byte).value.toDouble()) { a, n -> a / (n as byte).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as byte).value })
            }
        }
        args.all { it is short } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as short).value })
                mathOp.minus -> long(args.fold(0L) { a, n -> a - (n as short).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as short).value.toDouble()) { a, n -> a / (n as short).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as short).value })
            }
        }
        args.all { it is int } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as int).value })
                mathOp.minus -> long(args.fold(0L) { a, n -> a - (n as int).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as int).value.toDouble()) { a, n -> a / (n as int).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as int).value })
            }
        }
        args.all { it is float } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as float).value })
                mathOp.minus -> double(args.fold(0.0) { a, n -> a - (n as float).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as float).value.toDouble()) { a, n -> a / (n as float).value })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as float).value })
            }
        }
        args.all { it is double } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as double).value })
                mathOp.minus -> double(args.fold(0.0) { a, n -> a - (n as double).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as double).value) { a, n -> a / (n as double).value })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as double).value })
            }
        }
        else -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as number<*>).asDouble })
                mathOp.minus -> double(args.fold(0.0) { a, n -> a - (n as number<*>).asDouble })
                mathOp.div -> double(args.drop(1).fold((args.first() as number<*>).asDouble) { a, n -> a / (n as number<*>).asDouble })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as number<*>).asDouble })
            }
        }
    }
}

fun compare(op: compareOp, args: exps): Boolean {
    require(args.size == 2) { "$op should have 2 arguments, got ${args.size}" }
    val (x, y) = args
    require(args.all { it is number<*> }) { "$op should have numeric arguments" }
    x as number<*>
    y as number<*>
    return when (op) {
        compareOp.lte -> x.asDouble <= y.asDouble
        compareOp.lt -> x.asDouble < y.asDouble
        compareOp.gte -> x.asDouble >= y.asDouble
        compareOp.gt -> x.asDouble > y.asDouble
        compareOp.eq -> x.asDouble == y.asDouble
    }
}

fun isa(t: type, args: exps): exp {
    require(args.size == 1) { "$t? should have 1 argument, got ${args.size}" }
    val value = args.first()
    return bool(
            try {
                when (t) {
                    type.byte -> value as byte
                    type.short -> value as short
                    type.int -> value as int
                    type.long -> value as long
                    type.float -> value as float
                    type.double -> value as double
                    type.char -> value as char
                    type.string -> value as symbol
                    type.bool -> value as bool
                }
                true
            } catch (_: Throwable) {
                false
            }
    )
}

fun _is(args: exps): exp {
    require(args.size == 2) { "is? must have 2 arguments, got ${args.size}" }
    val (f, s) = args
    return bool(f === s)
}

fun eq(args: exps): exp {
    require(args.size == 2) { "eq? must have 2 arguments, got ${args.size}" }
    val (f, s) = args
    return bool(f == s)
}

fun range(args: exps): list {
    require(args.size == 2) { "range requires 2 arguments, got ${args.size}" }
    require(args.all { it is integer<*> }) { "range requires 2 integer arguments" }
    val (f, l) = args
    f as integer<*>
    l as integer<*>
    return list((f.asLong..l.asLong).map(::long))
}

fun procedure(params: exp, body: exp, env: env): exp {
    require(params is list) {"parameters should be a list"}
    return symbol("\nenv:\n$env\nparams:\n$params\nbody:\n$body")
}
