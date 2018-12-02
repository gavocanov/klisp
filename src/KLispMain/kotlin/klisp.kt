@file:Suppress("SpellCheckingInspection")

package klisp

import kotlinx.cinterop.toKString
import linenoise.linenoise
import linenoise.linenoiseHistoryAdd
import linenoise.linenoiseHistoryLoad
import linenoise.linenoiseHistorySave
import linenoise.linenoiseHistorySetMaxLen
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.getenv
import kotlin.system.getTimeNanos

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
                            float(s.toFloat())
                        } catch (_: Throwable) {
                            try {
                                double(s.toDouble())
                            } catch (_: Throwable) {
                                string(s)
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
            println(":took ${(e - s) / 1e6}ms (${e - s}ns)")
            r
        }

@Suppress("IMPLICIT_CAST_TO_ANY")
fun eval(x: exp, env: MutableMap<string, exp> = stdEnv): exp {
    return when {
        x is string && x.value == "profile" -> {
            PROFILE = !PROFILE
            bool(PROFILE)
        }
        x is string && (x.value == "env" || x.value == "ls") -> {
            env.forEach {
                val (k, _v) = it
                val v = when (_v) {
                    is func -> "function"
                    else -> _v
                }
                println("$k: $v")
            }
            unit
        }
        x is string -> try {
            env[x] as exp
        } catch (_: Throwable) {
            throw IllegalStateException("unknown symbole '$x'")
        }
        x is bool -> x
        x is number<*> -> x
        x is list && x.value[0] == string("unless") -> {
            val (_, test: exp, conseq) = x.value
            if (!(eval(test, env) as bool).value) eval(conseq, env) else unit
        }
        x is list && x.value[0] == string("when") -> {
            val (_, test: exp, conseq) = x.value
            if ((eval(test, env) as bool).value) eval(conseq, env) else unit
        }
        x is list && x.value[0] == string("if") -> {
            val (_, test: exp, conseq, alt) = x.value
            val exp = if ((eval(test, env) as bool).value) conseq else alt
            eval(exp, env)
        }
        x is list && x.value[0] == string("def") -> {
            val (_, s: exp, e) = x.value
            env[s as string] = eval(e, env)
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

fun getHistoryFileName(): String {
    val home = getenv("HOME")?.toKString() ?: throw IllegalStateException("failed to determine user home")
    return "$home/.kl_history"
}

fun loadHistory(fname: String): Boolean {
    val file = fopen(fname, "a+")
    fclose(file)
    linenoiseHistorySetMaxLen(64_000)
    val loaded = linenoiseHistoryLoad(fname) == 0
    if (!loaded)
        println("failed to load history file $fname")
    return loaded
}

fun saveToHistory(l: String, fname: String, save: Boolean = true) {
    linenoiseHistoryAdd(l)
    if (save) linenoiseHistorySave(fname)
}

fun readLine() =
        linenoise("kl -> ")?.toKString()

fun main(args: Array<String>) {
    val historyFileName = getHistoryFileName()
    val historyLoaded = loadHistory(historyFileName)

    while (true) {
        val line = readLine() ?: return exit(0)
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
