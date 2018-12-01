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
            val r = eval(parse(line))
            saveToHistory(line, historyFileName, historyLoaded)
            r
        } catch (t: Throwable) {
            t.message
        }
        println(res)
    }
}
