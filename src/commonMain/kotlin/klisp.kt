package klisp

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"

fun tokenize(s: String): List<String> {
    val p = s
            .replace("\n", "")
            .replace("(", " ( ")
            .replace(")", " ) ")
    return split(p)
            .also { if (DEBUG) println("parsed as: $it") }
}

// split by space not contained in "", '', [] and {}
fun split(s: String): List<String> =
        "[^\\s\"'{\\[]+|\"([^\"]*)\"|'([^']*)'|\\{([^{]*)}|\\[([^\\[]*)]"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

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
            _list(l)
        }
        ")" -> throw IllegalStateException("unexpected )")
        else -> parseAtom(token)
    }
}

fun parseAtom(s: String): atom = when {
    s.startsWith(':') -> keyword(s)
    s.startsWith('"') && s.endsWith('"') -> string(s)
    s.startsWith('\'') && s.endsWith('\'') && s.length == 3 -> char(s[1])
    s.startsWith('[') && s.endsWith(']') -> list(
            split(s.substringAfter("[").substringBeforeLast("]")).map(::parseAtom)
    )
    s.startsWith('{') && s.endsWith('}') -> {
        val l = split(s.substringAfter("{").substringBeforeLast("}"))
        val keys = l.filter { it.startsWith(':') }
        val vals = (l - keys)
        map(keys.zip(vals).map { (k, v) ->
            keyword(k) to parseAtom(v)
        }.toMap())
    }
    else -> {
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
    }
}

fun profile(fn: () -> Any): Any =
        if (!PROFILE) fn()
        else {
            val s = getTimeNanos()
            val r = fn()
            val e = getTimeNanos()
            println(":*took* ${((e - s) / 1e6)} ms")
            r
        }

fun eval(x: exp, env: env = stdEnv): exp = when {
    x is symbol && x.value == "debug" -> {
        DEBUG = !DEBUG
        bool(DEBUG)
    }
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
    x is keyword -> x
    x is bool -> x
    x is number<*> -> x
    x is string -> x
    x is char -> x
    x is _list && x.value[0] is keyword -> {
        val (k, v) = x.value
        val m = try {
            eval(v) as map
        } catch (_: Throwable) {
            throw IllegalArgumentException("second arguments should eval to a map")
        }
        m[k as keyword] ?: unit
    }
    x is _list && x.value[0] == symbol("map") -> {
        val (_, _exp, _list) = x.value
        val list = eval(_list) as coll
        val exp = eval(_exp)
        fmap(exp, list)
    }
    x is _list && x.value[0] == symbol("lambda") -> {
        val (_, params, body) = x.value
        lam(params, body, env)
    }
    x is _list && x.value[0] == symbol("quote") -> {
        val (_, exp) = x.value
        exp
    }
    x is _list && x.value[0] == symbol("unless") -> {
        val (_, test: exp, conseq) = x.value
        if (!(eval(test, env) as bool).value) eval(conseq, env) else unit
    }
    x is _list && x.value[0] == symbol("when") -> {
        val (_, test: exp, conseq) = x.value
        if ((eval(test, env) as bool).value) eval(conseq, env) else unit
    }
    x is _list && x.value[0] == symbol("if") -> {
        val (_, test: exp, conseq, alt) = x.value
        val exp = if ((eval(test, env) as bool).value) conseq else alt
        eval(exp, env)
    }
    x is _list && (x.value[0] == symbol("def") || x.value[0] == symbol("define")) -> {
        val (_, s: exp, e) = x.value
        env[s as symbol] = eval(e, env)
        env[s] as exp
    }
    x is coll -> x
    x is map -> x
    else -> {
        x as _list
        val exp = x.value[0]
        val proc = try {
            eval(exp, env) as func
        } catch (_: Throwable) {
            throw IllegalArgumentException("first argument should be a function")
        }
        val args = x.value.drop(1).map { eval(it, env) }
        try {
            val res = proc.func(args)
            env[symbol("$")] = res
            res
        } catch (t: Throwable) {
            throw t.cause ?: t
        }
    }
}

fun main(args: Array<String>) {
    println("**klisp ${version()}**")

    val historyFileName = getHistoryFileName()
    val historyLoaded = loadHistory(historyFileName)

    while (true) {
        val line = try {
            readLine("kl -> ")
        } catch (t: Throwable) {
            println(t.message ?: t::class.simpleName)
            exit(0)
            ""
        } as String
        val res = try {
            val r = profile { eval(parse(line)) }
            saveToHistory(line, historyFileName, historyLoaded)
            r
        } catch (t: Throwable) {
            t.message ?: t
        }
        println(res)
    }
}
