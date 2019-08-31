@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp

fun eval(x: exp, lsp: Boolean, env: env = stdEnv): exp {
    if (_DEBUG && !lsp) LOGGER.debug(":eval <$x>")
    return when {
        x is unit -> x
        x is symbol && specialForm.isSpecial(x.value) -> {
            when (specialForm.from(x.value)) {
                specialForm.DEBUG -> {
                    _DEBUG = !_DEBUG
                    bool(_DEBUG)
                }
                specialForm.PROFILE -> {
                    _PROFILE = !_PROFILE
                    bool(_PROFILE)
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
                eval(v, lsp) as map
            } catch (_: Throwable) {
                throw IllegalArgumentException("second arguments should eval to a map")
            }
            m[k as keyword] ?: unit
        }
        x is _list && x[0] is symbol && specialForm.isSpecial(x[0] as symbol) -> {
            when (specialForm.fromSymbol(x[0] as symbol)) {
                specialForm.DEF -> {
                    val (_, s: exp, e) = x
                    env[s as symbol] = eval(e, lsp, env)
                    env[s] as exp
                }
                specialForm.IF -> {
                    val (_, test: exp, conseq, alt) = x
                    val exp = when (val res = eval(test, lsp, env)) {
                        is bool -> res
                        is collection -> bool(res.isNotEmpty())
                        is number<*> -> bool(res.asDouble > 0.0)
                        is map -> bool(res.isNotEmpty())
                        is string -> bool(res.value.isNotEmpty())
                        else -> bool(true)
                    }
                    val conRes = if (exp.value) conseq else alt
                    eval(conRes, lsp, env)
                }
                specialForm.UNLESS -> {
                    val (_, test: exp, conseq) = x
                    if (!(eval(test, lsp, env) as bool).value) eval(conseq, lsp, env) else unit
                }
                specialForm.WHEN -> {
                    val (_, test: exp, conseq) = x
                    if ((eval(test, lsp, env) as bool).value) eval(conseq, lsp, env) else unit
                }
                specialForm.FMAP -> {
                    val (_, _exp, _list) = x
                    val list = eval(_list, lsp) as collection
                    val exp = eval(_exp, lsp)
                    fmap(exp, list)
                }
                specialForm.LAMBDA -> {
                    val (_, params, body) = x
                    lam(params, body, lsp, env)
                }
                specialForm.QUOTE -> {
                    val (_, exp) = x
                    when (exp) {
                        is _list -> list(exp.toList())
                        else -> exp
                    }
                }
                specialForm.FILTER -> {
                    val (_, _exp, _list) = x
                    val list = eval(_list, lsp) as collection
                    val exp = eval(_exp, lsp)
                    filter(exp, list)
                }
                specialForm.REDUCE -> {
                    val (_, _id, _exp, _list) = x
                    val list = eval(_list, lsp) as collection
                    val id = eval(_id, lsp)
                    val exp = eval(_exp, lsp)
                    reduce(id, exp, list)
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
                eval(exp, lsp, env) as func
            } catch (_: Throwable) {
                throw IllegalArgumentException("first argument should be a function")
            }

            if (_PROFILE && _DEBUG) proc.meta = x

            val args = x.drop(1).map { eval(it, lsp, env) }
            try {
                val res = proc(args)
                check(res !is _list) { "res is a _list, that's a bug" }
                res
            } catch (t: Throwable) {
                throw t.cause ?: t
            }
        }
    }
}

