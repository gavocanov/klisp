package klisp

fun begin(args: exps): exp {
    require(args.size > 1) { "begin should have at least 1 argument" }
    return args.last()
}

fun foldableMathOp(op: mathOp, args: exps): exp {
    require(args.size >= 2) { "$op expects at least 2 arguments" }
    return when {
        args.all { it is byte } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as byte).value })
                mathOp.minus -> long(args.drop(1).fold((args.first() as byte).value.toLong()) { a, n -> a - (n as byte).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as byte).value.toDouble()) { a, n -> a / (n as byte).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as byte).value })
            }
        }
        args.all { it is short } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as short).value })
                mathOp.minus -> long(args.drop(1).fold((args.first() as short).value.toLong()) { a, n -> a - (n as short).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as short).value.toDouble()) { a, n -> a / (n as short).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as short).value })
            }
        }
        args.all { it is int } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as int).value })
                mathOp.minus -> long(args.drop(1).fold((args.first() as int).value.toLong()) { a, n -> a - (n as int).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as int).value.toDouble()) { a, n -> a / (n as int).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as int).value })
            }
        }
        args.all { it is long } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as long).value })
                mathOp.minus -> long(args.drop(1).fold((args.first() as long).value) { a, n -> a - (n as long).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as long).value.toDouble()) { a, n -> a / (n as long).value })
                mathOp.mul -> long(args.fold(1L) { a, n -> a * (n as long).value })
            }
        }
        args.all { it is float } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as float).value })
                mathOp.minus -> double(args.drop(1).fold((args.first() as float).value.toDouble()) { a, n -> a - (n as float).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as float).value.toDouble()) { a, n -> a / (n as float).value })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as float).value })
            }
        }
        args.all { it is double } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as double).value })
                mathOp.minus -> double(args.drop(1).fold((args.first() as double).value) { a, n -> a - (n as double).value })
                mathOp.div -> double(args.drop(1).fold((args.first() as double).value) { a, n -> a / (n as double).value })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as double).value })
            }
        }
        args.all { it is number<*> } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as number<*>).asDouble })
                mathOp.minus -> double(args.drop(1).fold((args.first() as number<*>).asDouble) { a, n -> a - (n as number<*>).asDouble })
                mathOp.div -> double(args.drop(1).fold((args.first() as number<*>).asDouble) { a, n -> a / (n as number<*>).asDouble })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as number<*>).asDouble })
            }
        }
        args.all { it is string || it is number<*> } -> {
            when (op) {
                mathOp.plus -> {
                    val s = args.joinToString("") { e ->
                        when (e) {
                            is number<*> -> e.value.toString()
                            is string -> e.value.replace("\"", "")
                            else -> throw IllegalStateException("this should not be....")
                        }
                    }
                    string("\"$s\"")
                }
                mathOp.minus -> TODO()
                mathOp.div -> TODO()
                mathOp.mul -> TODO()
            }
        }
        else ->
            throw IllegalArgumentException("$op for arguments of type <${args.map { it::class.simpleName }.joinToString(", ")}> is not supported")
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
                    type.string -> value as string
                    type.bool -> value as bool
                    type.keyword -> value as keyword
                    type.list -> value as list
                    type.set -> value as set
                    type.map -> value as map
                    type.coll -> value as coll
                    type.number -> value as number<*>
                    type.integer -> value as integer<*>
                    type.decimal -> value as decimal<*>
                    type.symbol -> value as symbol
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

fun range(args: exps): coll {
    require(args.size == 2) { "range requires 2 arguments, got ${args.size}" }
    require(args.all { it is integer<*> }) { "range requires 2 integer arguments" }
    val (f, l) = args
    f as integer<*>
    l as integer<*>
    return list((f.asLong..l.asLong).map(::long))
}

@Suppress("USELESS_CAST") // needed for native target
fun lam(argNames: exp, body: exp, env: env): exp {
    require(argNames is _list) { "arguments should be a list" }
    argNames as _list
    require(argNames.value.all { it is symbol }) { "argument names should all be valid symbols" }
    require(body is _list) { "body should be a list" }
    val _argNames = argNames.value.map { it as symbol }
    return func { argVals ->
        val map = ChainMap(env)
        map.putAll(_argNames.zip(argVals))
        eval(body, map)
    }
}

@Suppress("USELESS_CAST") // needed for native target
fun fmap(exp: exp, list: exp): exp {
    require(list is coll) { "second argument should be a collection" }
    list as coll
    return when (exp) {
        is func -> {
            val f = exp.func
            list(list.value.map { f(listOf(it)) })
        }
        is atom -> list(list.value.map { exp })
        is _list -> throw IllegalStateException("this should not be...")
    }
}

fun set(it: List<exp>): exp = when {
    it.first() is list -> set((it.first() as list).value.toSet())
    else -> set(it.toSet())
}

