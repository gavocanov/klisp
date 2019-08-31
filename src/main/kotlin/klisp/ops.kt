@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package klisp

import klisp.parser.lexer.KLispLexer
import klisp.parser.lexer.LiveStream
import kotlin.math.absoluteValue
import kotlin.math.pow

fun begin(args: exps): exp = args.last()

fun foldableMathOp(op: mathOp, args: exps): exp {
    require(args.isNotEmpty()) { "$op expects at least 1 argument" }

    return when (val res = when {
        args.all { it is integer<*> } && args.any { (it as integer<*>).asLong == Long.MAX_VALUE } -> {
            when (op) {
                mathOp.plus -> ulong(args.fold(0UL) { a, n -> a + (n as integer<*>).numericValue.toString().toULong() })
                mathOp.minus -> ulong(args.drop(1).fold((args.first() as integer<*>).numericValue.toString().toULong()) { a, n -> a - (n as integer<*>).numericValue.toString().toULong() })
                mathOp.div -> double(args.drop(1).fold((args.first() as integer<*>).asDouble) { a, n -> a / (n as integer<*>).asDouble })
                mathOp.mul -> ulong(args.fold(1UL) { a, n -> a * (n as integer<*>).numericValue.toString().toULong() })
                mathOp.pow -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as integer<*>).asDouble.pow((args[1] as integer<*>).asDouble))
                }
                mathOp.rem -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as integer<*>).asDouble.rem((args[1] as integer<*>).asDouble))
                }
                mathOp.abs -> {
                    require(args.size == 1) { "$op expects exactly 1 parameter" }
                    args[0] as ulong
                }
            }
        }
        args.all { it is integer<*> } -> {
            when (op) {
                mathOp.plus -> long(args.fold(0L) { a, n -> a + (n as integer<*>).asLong })
                mathOp.minus -> long(args.drop(1).fold((args.first() as integer<*>).asLong) { a, n -> a - (n as integer<*>).asLong })
                mathOp.div -> double(args.drop(1).fold((args.first() as integer<*>).asDouble) { a, n -> a / (n as integer<*>).asLong })
                mathOp.mul -> ulong(args.fold(1UL) { a, n -> a * (n as integer<*>).numericValue.toString().toULong() })
                mathOp.pow -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as integer<*>).asDouble.pow((args[1] as integer<*>).asDouble))
                }
                mathOp.rem -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as integer<*>).asDouble.rem((args[1] as integer<*>).asDouble))
                }
                mathOp.abs -> {
                    require(args.size == 1) { "$op expects exactly 1 parameter" }
                    long((args[0] as integer<*>).asLong.absoluteValue)
                }
            }
        }
        args.all { it is number<*> } -> {
            when (op) {
                mathOp.plus -> double(args.fold(0.0) { a, n -> a + (n as number<*>).asDouble })
                mathOp.minus -> double(args.drop(1).fold((args.first() as number<*>).asDouble) { a, n -> a - (n as number<*>).asDouble })
                mathOp.div -> double(args.drop(1).fold((args.first() as number<*>).asDouble) { a, n -> a / (n as number<*>).asDouble })
                mathOp.mul -> double(args.fold(1.0) { a, n -> a * (n as number<*>).asDouble })
                mathOp.pow -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as number<*>).asDouble.pow((args[1] as number<*>).asDouble))
                }
                mathOp.rem -> {
                    require(args.size == 2) { "$op expects exactly 2 parameters" }
                    double((args[0] as number<*>).asDouble.rem((args[1] as number<*>).asDouble))
                }
                mathOp.abs -> {
                    require(args.size == 1) { "$op expects exactly 1 parameter" }
                    double((args[0] as number<*>).asDouble.absoluteValue)
                }
            }
        }
        args.all { it is string || it is number<*> || it is bool } -> {
            when (op) {
                mathOp.plus -> {
                    val s = args.joinToString("") { e ->
                        when (e) {
                            is number<*> -> e.numericValue.toString()
                            is string -> e.value.replace("\"", "")
                            is bool -> e.integerValue.toString()
                            else -> throw IllegalStateException("this should not be....")
                        }
                    }
                    string("\"$s\"")
                }
                else -> throw IllegalArgumentException("$op for arguments of type <${args.map { it::class.simpleName }.joinToString(", ")}> is not supported")
            }
        }
        else ->
            throw IllegalArgumentException("$op for arguments of type <${args.map { it::class.simpleName }.joinToString(", ")}> is not supported")
    }) {
        is long -> if (res.value == Long.MAX_VALUE || res.value == Long.MIN_VALUE) throw IllegalStateException("under/overflow res <$res>, args <$args>, op <$op>") else res
        is ulong -> if (res.value == ULong.MAX_VALUE || res.value == ULong.MIN_VALUE) throw IllegalStateException("under/overflow res <$res>, args <$args>, op <$op>") else res
        is double -> if (res.value == Double.POSITIVE_INFINITY || res.value == Double.NEGATIVE_INFINITY) throw IllegalStateException("under/overflow res <$res>, args <$args>, op <$op>") else res
        is string -> res
        else -> throw IllegalStateException("result <$res> is unexpected for arguments <$args> and op <$op>")
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
    require(args.size == 1) { "${t.toString().tail}? should have 1 argument, got ${args.size}" }
    val value = args.first()
    return bool(
        try {
            when (t) {
                type._byte -> value as byte
                type._short -> value as short
                type._int -> value as int
                type._long -> value as long
                type._float -> value as float
                type._double -> value as double
                type._char -> value as char
                type._string -> value as string
                type._bool -> value as bool
                type._keyword -> value as keyword
                type._list -> value as list
                type._set -> value as set
                type._map -> value as map
                type._collection -> value as collection
                type._number -> value as number<*>
                type._integer -> value as integer<*>
                type._decimal -> value as decimal<*>
                type._symbol -> value as symbol
                type._atom -> value as atom
                type._ubyte -> value as ubyte
                type._ushort -> value as ushort
                type._uint -> value as uint
                type._ulong -> value as ulong
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

fun range(args: exps): collection {
    require(args.size == 2) { "range requires 2 arguments, got ${args.size}" }
    require(args.all { it is integer<*> }) { "range requires 2 integer arguments" }
    val (f, l) = args
    f as integer<*>
    l as integer<*>
    return list((f.asLong..l.asLong).map(::long))
}

fun lam(argNames: exp, body: exp, lsp: Boolean, env: env): exp {
    require(argNames is _list) { "arguments should be a list" }
    require(argNames.all { it is symbol }) { "argument names should all be valid symbols" }
    require(body is _list) { "body should be a list" }
    val _argNames = argNames.map { it as symbol }
    return func { argVals ->
        val map = ChainMap(env)
        map.putAll(_argNames.zip(argVals))
        eval(body, lsp, map)
    }
}

fun fmap(exp: exp, list: exp): exp {
    require(list is collection) { "second argument should be a collection" }
    return when (exp) {
        is func -> list(list.map { exp(listOf(it)) })
        is atom -> list(list.map { exp })
        else -> throw IllegalStateException("this should not be...")
    }
}

fun set(it: exps): exp = when {
    it.first() is list -> set((it.first() as list).toSet())
    else -> set(it.toSet())
}

fun lex(args: exps): string {
    require(args.size == 1) { "lex should have 1 argument, got ${args.size}" }
    require(args[0] is string) { "argument should be a string" }
    val s = args.first() as string
    val stream = LiveStream(s.value.drop(1).dropLast(1))
    val lexer = KLispLexer()
    lexer.lex(stream)
    return string(lexer.output.toList.toString())
}

fun json(args: exps): string {
    require(args.size == 1) { "json should have 1 argument, got ${args.size}" }
    require(args[0] is map) { "argument should be a map" }
    val map = args.first()
    return string(map.toJson())
}

fun map(it: exps): exp {
    val keys = it.filterIsInstance<keyword>()
    val vals = it.filter { it !is keyword }
    require(vals.size == keys.size) { "invalid k/v count <k:${keys.size}, v:${vals.size}>" }
    return map(value = keys.zip(vals).toMap())
}

fun filter(exp: exp, list: exp): exp {
    require(list is collection) { "second argument should be a collection" }
    return when (exp) {
        is func -> list(list.filter {
            (exp(listOf(it)) as bool).value
        })
        else -> throw IllegalStateException("this should not be...")
    }
}

fun reduce(id: exp, exp: exp, list: exp): exp {
    require(list is collection) { "third argument should be a collection" }
    return when (exp) {
        is func -> list.fold(id) { a, n ->
            (exp(listOf(a, n)))
        }
        else -> throw IllegalStateException("this should not be...")
    }
}

