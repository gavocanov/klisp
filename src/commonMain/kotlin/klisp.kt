package klisp

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"

fun tokenize(s: String, dump: Boolean = false): List<String> {
    val parsed = s
            .replace("\n", "")
            .replace("(", " ( ")
            .replace(")", " ) ")
    val tokens = split(parsed)
    if (dump) println("parsed as: $tokens")
    return tokens
}

// split by space not contained in "", '', [] and {}
fun split(s: String): List<String> =
        "[^\\s\"'{\\[]+|\"([^\"]*)\"|'([^']*)'|\\{([^{]*)}|\\[([^\\[]*)]"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

fun parse(s: String): exp =
        readFromTokens(tokenize(s, DEBUG).toMutableList())

fun readFromTokens(tokens: MutableList<String>): exp {
    if (tokens.isEmpty()) throw IllegalArgumentException("tokens list is empty")
    val token = tokens.first()
    tokens.removeAt(0)
    return when (token) {
        "(" -> {
            val list = mutableListOf<exp>()
            try {
                while (tokens[0] != ")")
                    list.add(readFromTokens(tokens))
                tokens.removeAt(0)
            } catch (t: Throwable) {
                throw IllegalArgumentException("parsing failed")
            }
            _list(list)
        }
        ")" -> throw IllegalArgumentException("unexpected )")
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
        val list = split(s.substringAfter("{").substringBeforeLast("}"))
        val keys = list.filter { it.startsWith(':') }
        val vals = (list - keys)
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

fun eval(x: exp, env: env = stdEnv): exp = when {
    x is symbol && specialForm.isSpecial(x.value) -> {
        when (specialForm.fromString(x.value)) {
            specialForm.DEBUG -> {
                DEBUG = !DEBUG
                bool(DEBUG)
            }
            specialForm.PROFILE -> {
                PROFILE = !PROFILE
                bool(PROFILE)
            }
            specialForm.ENV -> {
                env.forEach { (k, v) -> println("$k -> $v") }
                unit
            }
            else -> throw IllegalArgumentException("unknown symbol <${x.value}>")
        }
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
    x is _list && specialForm.isSpecial(x.value[0] as symbol) -> {
        when (specialForm.fromSymbol(x.value[0] as symbol)) {
            specialForm.DEF -> {
                val (_, s: exp, e) = x.value
                env[s as symbol] = eval(e, env)
                env[s] as exp
            }
            specialForm.IF -> {
                val (_, test: exp, conseq, alt) = x.value
                val exp = if ((eval(test, env) as bool).value) conseq else alt
                eval(exp, env)
            }
            specialForm.UNLESS -> {
                val (_, test: exp, conseq) = x.value
                if (!(eval(test, env) as bool).value) eval(conseq, env) else unit
            }
            specialForm.WHEN -> {
                val (_, test: exp, conseq) = x.value
                if ((eval(test, env) as bool).value) eval(conseq, env) else unit
            }
            specialForm.MAP -> {
                val (_, _exp, _list) = x.value
                val list = eval(_list) as coll
                val exp = eval(_exp)
                fmap(exp, list)
            }
            specialForm.LAMBDA -> {
                val (_, params, body) = x.value
                lam(params, body, env)
            }
            specialForm.QUOTE -> {
                val (_, exp) = x.value
                exp
            }
            else -> throw IllegalArgumentException("unknown symbol <${x.value[0]}> in expression <$x>")
        }
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
        } catch (exit: ExitException) {
            println("bye!!")
            null
        } catch (t: Throwable) {
            println(t.message ?: t::class.simpleName)
            null
        }

        if (line !== null) {
            val res = try {
                val r = eval(parse(line))
                saveToHistory(line, historyFileName, historyLoaded)
                r
            } catch (t: Throwable) {
                err(t.message ?: t::class.simpleName)
            }
            println(res)
        } else exit(0)
    }
}
