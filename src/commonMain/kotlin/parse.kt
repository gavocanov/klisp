package klisp

import kotlinx.serialization.ImplicitReflectionSerializer

// split by space not contained in "", ''
fun split(s: String): List<String> =
        "[^\\s\"']+|\"([^\"]*)\"|'([^']*)'"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

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

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
fun parse(s: String): exp =
        readFromTokens(tokenize(s, DEBUG).toMutableList())

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
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
@ImplicitReflectionSerializer
fun parseAtom(s: String): atom = when {
    s.startsWith(':') -> keyword(s)
    s.startsWith('"') && s.endsWith('"') -> string(s)
    s.startsWith('\'') && s.endsWith('\'') && s.length == 3 -> char(s[1])
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

