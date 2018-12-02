package klisp

var PROFILE = false

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

fun parseAtom(s: String): atom =
        try {
            byte(s.toByte())
        } catch (_: Throwable) {
            try {
                short(s.toShort())
            } catch (_: Throwable) {
                try {
                    int(s.toInt())
                } catch (_: Throwable) {
                    try {
                        long(s.toLong())
                    } catch (_: Throwable) {
                        try {
                            val f = float(s.toFloat())
                            if (f.value.isInfinite() || f.value.isNaN())
                                throw IllegalStateException()
                            else f
                        } catch (_: Throwable) {
                            try {
                                val d = double(s.toDouble())
                                if (d.value.isInfinite() || d.value.isNaN())
                                    throw IllegalStateException()
                                else d
                            } catch (_: Throwable) {
                                symbol(s)
                            }
                        }
                    }
                }
            }
        }

fun profile(fn: () -> Any): Any =
        if (!PROFILE)
            fn()
        else {
            val s = getTimeNanos()
            val r = fn()
            val e = getTimeNanos()
            println(":*took* ${((e - s) / 1e6)} ms")
            r
        }

@Suppress("IMPLICIT_CAST_TO_ANY")
fun eval(x: exp, env: MutableMap<symbol, exp> = stdEnv): exp {
    return when {
        x is symbol && x.value == "profile" -> {
            PROFILE = !PROFILE
            bool(PROFILE)
        }
        x is symbol && (x.value == "env" || x.value == "ls") -> {
            env.forEach { (k, v) ->
                println("$k -> $v")
            }
            unit
        }
        x is symbol -> try {
            env[x] as exp
        } catch (_: Throwable) {
            throw IllegalStateException("unknown symbol <${x.value}>")
        }
        x is bool -> x
        x is number<*> -> x
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
            env[s] as exp
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
    val historyFileName = getHistoryFileName()
    val historyLoaded = loadHistory(historyFileName)

    while (true) {
        val line = readLine("kl -> ") ?: return exit(0)
        val res = try {
            val r = profile { eval(parse(line)) }
            saveToHistory(line, historyFileName, historyLoaded)
            r
        } catch (t: Throwable) {
            t.message
        }
        println(res)
    }
}
