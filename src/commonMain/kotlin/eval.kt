package klisp

import kotlinx.serialization.ImplicitReflectionSerializer

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
fun eval(x: exp, env: env = stdEnv): exp {
    if (DEBUG) LOGGER.debug(":eval <$x>")
    return when {
        x is unit -> x
        x is symbol && specialForm.isSpecial(x.value) -> {
            when (specialForm.from(x.value)) {
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
                specialForm.FMAP -> {
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
                specialForm.FILTER -> {
                    val (_, _exp, _list) = x
                    val list = eval(_list) as collection
                    val exp = eval(_exp)
                    filter(exp, list)
                }
                specialForm.REDUCE -> {
                    val (_, _id, _exp, _list) = x
                    val list = eval(_list) as collection
                    val id = eval(_id)
                    val exp = eval(_exp)
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

