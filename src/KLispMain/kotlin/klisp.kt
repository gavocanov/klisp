@file:Suppress("ClassName")
package klisp

import kotlin.math.PI

sealed class exp
sealed class atom : exp()
sealed class number : atom()

object unit : atom()
data class symbol(val value: String) : atom()
data class bool(val value: Boolean) : atom()
data class list(val value: List<exp>) : exp()
data class func(val func: (List<exp>) -> exp) : exp()
data class int(val value: Int) : number()
data class float(val value: Float) : number()

fun tokenize(s: String) = s
        .replace("\n", "")
        .replace("(", " ( ")
        .replace(")", " ) ")
        .split(" ")
        .filter(String::isNotBlank)

fun parse(s: String): exp =
        readFromTokens(tokenize(s).toMutableList())

fun readFromTokens(tokens: MutableList<String>): exp {
    if (tokens.isEmpty()) throw IllegalArgumentException("tokens list is empty")
    val token = tokens.first()
    tokens.removeAt(0)
    return when (token) {
        "(" -> {
            val l = mutableListOf<exp>()
            while (tokens[0] != ")")
                l.add(readFromTokens(tokens))
            tokens.removeAt(0)
            list(l)
        }
        ")" -> throw IllegalStateException("unexpected )")
        else -> parseAtom(token)
    }
}

fun parseAtom(s: String): atom = try {
    int(s.toInt())
} catch (_: Throwable) {
    try {
        float(s.toFloat())
    } catch (_: Throwable) {
        symbol(s)
    }
}

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

val stdEnv: MutableMap<symbol, exp> = mutableMapOf(
        symbol("pi") to float(PI.toFloat()),
        symbol("true") to bool(true),
        symbol("false") to bool(false),
        symbol("begin") to func(::begin),
        symbol("*") to func(::mul),
        symbol("+") to func(::plus),
        symbol("-") to func(::minus),
        symbol("/") to func(::div),
        symbol(">") to func(::gt),
        symbol(">=") to func(::gte),
        symbol("<") to func(::lt),
        symbol("<=") to func(::lte),
        symbol("string?") to func(::isString),
        symbol("is?") to func(::isa),
        symbol("eq?") to func(::eq)
)

fun eval(x: exp, env: MutableMap<symbol, exp> = stdEnv): exp {
    return when {
        x is symbol -> try {
            env[x] as exp
        } catch (_: Throwable) {
            throw IllegalStateException("unknown symbol '$x'")
        }
        x is bool -> x
        x is number -> x
        x is list && x.value[0] == symbol("unless") -> {
            val (_, test: exp, conseq) = x.value
            if (!(eval(test, env) as bool).value) eval(conseq, env) else unit
        }
        x is list && x.value[0] == symbol("when") -> {
            val (_, test: exp, conseq) = x.value
            if ((eval(test, env) as bool).value) eval(conseq, env) else unit
        }
        x is list && x.value[0] == symbol("if") -> {
            val (_, test: exp, conseq, alt) = x.value
            val exp = if ((eval(test, env) as bool).value) conseq else alt
            eval(exp, env)
        }
        x is list && x.value[0] == symbol("def") -> {
            val (_, s: exp, e) = x.value
            env[s as symbol] = eval(e, env)
            unit
        }
        else -> {
            x as list
            val exp = x.value[0]
            val proc = eval(exp, env)
            val args = x.value.drop(1).map { eval(it, env) }
            try {
                (proc as func).func(args)
            } catch (t: Throwable) {
                throw t.cause ?: t
            }
        }
    }
}

fun main(args: Array<String>) {
    while (true) {
        print("kl -> ")
        val s = readLine() ?: throw IllegalStateException("failed to read input")
        val res = try {
            eval(parse(s))
        } catch (t: Throwable) {
            t.message
        }
        println(res)
    }
}

