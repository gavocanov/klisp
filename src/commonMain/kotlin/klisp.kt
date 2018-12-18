package klisp

var PROFILE = false
var DEBUG = false
const val HISTORY_FILE_NAME = ".kl_history"

fun tokenize(s: String, dump: Boolean = false): List<String> {
    val parsed = s
            .replace(Regex(";(.*)"), "")
            .replace("\n", "")
            .replace("(", " ( ")
            .replace(")", " ) ")
    val tokens = split(parsed)
    if (dump) LOGGER.debug("parsed as: $tokens")
    return tokens
}

// split by space not contained in "", '', [] and {}
fun split(s: String): List<String> =
        "[^\\s\"'{\\[]+|\"([^\"]*)\"|'([^']*)'|\\{([^{]*)}|\\[([^\\[]*)]"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

@ExperimentalUnsignedTypes
fun parse(s: String): exp =
        readFromTokens(tokenize(s, DEBUG).toMutableList())

@ExperimentalUnsignedTypes
fun readFromTokens(tokens: MutableList<String>): exp {
    if (tokens.isEmpty()) return unit
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

@ExperimentalUnsignedTypes
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
        val constructors = type
                .values()
                .filter(type::isNumber)
                .mapNotNull(type::constructor)

        var value: atom? = null
        for (f in constructors) {
            value = f(s) as atom
            if (value !== nil) break
        }

        if (value === nil || value === null) symbol(s)
        else value
    }
}

@ExperimentalUnsignedTypes
fun eval(x: exp, env: env = stdEnv): exp {
    if (DEBUG) LOGGER.debug(":eval <$x>")
    return when {
        x is unit -> x
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
                    env.forEach { (k, v) -> LOGGER.trace("$k -> $v") }
                    unit
                }
                else -> throw IllegalArgumentException("unknown symbol <$x>")
            }
        }
        x is symbol -> try {
            env[x] as exp
        } catch (_: Throwable) {
            throw IllegalStateException("unknown symbol <$x>")
        }
        x is keyword -> x
        x is bool -> x
        x is number<*> -> x
        x is string -> x
        x is char -> x
        x is _list && x[0] is keyword -> {
            val (k, v) = x
            val m = try {
                eval(v) as map
            } catch (_: Throwable) {
                throw IllegalArgumentException("second arguments should eval to a map")
            }
            m[k as keyword] ?: unit
        }
        x is _list && x[0] is symbol && specialForm.isSpecial(x[0] as symbol) -> {
            when (specialForm.fromSymbol(x[0] as symbol)) {
                specialForm.DEF -> {
                    val (_, s: exp, e) = x
                    env[s as symbol] = eval(e, env)
                    env[s] as exp
                }
                specialForm.IF -> {
                    val (_, test: exp, conseq, alt) = x
                    val res = eval(test, env)
                    val exp = when (res) {
                        is bool -> res
                        is collection -> bool(res.isNotEmpty())
                        is number<*> -> bool(res.asDouble > 0.0)
                        is map -> bool(res.isNotEmpty())
                        is string -> bool(res.value.isNotEmpty())
                        else -> bool(true)
                    }
                    val conRes = if (exp.value) conseq else alt
                    eval(conRes, env)
                }
                specialForm.UNLESS -> {
                    val (_, test: exp, conseq) = x
                    if (!(eval(test, env) as bool).value) eval(conseq, env) else unit
                }
                specialForm.WHEN -> {
                    val (_, test: exp, conseq) = x
                    if ((eval(test, env) as bool).value) eval(conseq, env) else unit
                }
                specialForm.MAP -> {
                    val (_, _exp, _list) = x
                    val list = eval(_list) as collection
                    val exp = eval(_exp)
                    fmap(exp, list)
                }
                specialForm.LAMBDA -> {
                    val (_, params, body) = x
                    lam(params, body, env)
                }
                specialForm.QUOTE -> {
                    val (_, exp) = x
                    when (exp) {
                        is _list -> list(exp.toList())
                        else -> exp
                    }
                }
                else -> throw IllegalArgumentException("unknown symbol <${x[0]}> in expression <$x>")
            }
        }
        x is collection -> x
        x is map -> x
        else -> {
            x as _list
            val exp = x[0]
            val proc = try {
                eval(exp, env) as func
            } catch (_: Throwable) {
                throw IllegalArgumentException("first argument should be a function")
            }

            if (PROFILE && DEBUG) proc.meta = x

            val args = x.drop(1).map { eval(it, env) }
            try {
                val res = proc(args)
                if (res is _list) throw IllegalStateException("res is a _list, that's a bug")
                res
            } catch (t: Throwable) {
                throw t.cause ?: t
            }
        }
    }
}

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    LOGGER.info("**klisp ${Platform.version()}**")

    val historyFileName = Platform.getHistoryFileName()
    val historyLoaded = Platform.loadHistory(historyFileName)

    while (true) {
        val line = try {
            Platform.readLine("kl${Platform.version().first()} -> ")
        } catch (exit: ExitException) {
            LOGGER.info("bye!!")
            null
        } catch (t: Throwable) {
            LOGGER.error(t.message ?: t::class.simpleName)
            null
        }

        if (line !== null) {
            val res = try {
                val _start = if (PROFILE) Platform.getTimeNanos() else 0
                val r = eval(parse(line))
                if (PROFILE) LOGGER.trace(":eval/parse ${took(_start)}")
                Platform.saveToHistory(line, historyFileName, historyLoaded)
                r
            } catch (t: Throwable) {
                err(t.message ?: t::class.simpleName)
            }
            if (res is err) LOGGER.error(res.toString())
            else LOGGER.info(res.toString())
        } else Platform.exit(0)
    }
}
