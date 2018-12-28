@file:Suppress("unused")

package klisp

import klisp.tty.TermColors

fun took(_start: Long) = "took ${Platform.strFormat(((Platform.getTimeNanos() - _start) / 1e6))} ms"

fun tryOrNil(f: () -> exp) = try {
    f()
} catch (_: Throwable) {
    nil
}

/**
 * split by space not contained in "", '', [] and {}
 */
@Suppress("unused")
fun splitNotSurrounded(s: String): List<String> =
        "[^\\s\"'{\\[]+|\"([^\"]*)\"|'([^']*)'|\\{([^{]*)}|\\[([^\\[]*)]"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

object LOGGER {
    private val C = TermColors()
    fun trace(msg: Any?) = println(C.gray(msg.toString()))
    fun debug(msg: Any?) = println(C.magenta(msg.toString()))
    fun info(msg: Any?) = println(C.blue(msg.toString()))
    fun warn(msg: Any?) = println(C.yellow(msg.toString()))
    fun error(msg: Any?) = println(C.red(msg.toString()))
}

class ChainMap<K, V>(private val map: MutableMap<K, V>) : MutableMap<K, V> {
    private val innerMap = mutableMapOf<K, V>()

    // those are just proxies and should not be used, here for completeness

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = (innerMap.entries + map.entries).toMutableSet()
    override val keys: MutableSet<K> = (innerMap.keys + map.keys).toMutableSet()
    override val values: MutableCollection<V> = (innerMap.values + map.values).toMutableList()

    // from here on we deal with the stuff

    override val size: Int = innerMap.size + map.size
    override fun clear() = innerMap.clear()
    override fun containsKey(key: K): Boolean = innerMap.containsKey(key) || map.containsKey(key)
    override fun containsValue(value: V): Boolean = innerMap.containsValue(value) || map.containsValue(value)
    override fun get(key: K): V? = innerMap[key] ?: map[key]
    override fun isEmpty(): Boolean = innerMap.isEmpty() && map.isEmpty()
    override fun put(key: K, value: V): V? = innerMap.put(key, value)
    override fun putAll(from: Map<out K, V>) = innerMap.putAll(from)
    override fun remove(key: K): V? = innerMap.remove(key)
}

/**
 * monoidal parsing from Edward Kmett (in haskell)
 * https://www.youtube.com/watch?v=Txf7swrcLYs&t=661s
 */
private typealias B = Pair<Int, Int>

private typealias PP = Pair<Char, Char>

typealias Result<T> = Pair<Boolean, T>

/**
 * semigroup for B
 */
private operator fun B.plus(that: B): B {
    val (l1, r1) = this
    val (l2, r2) = that
    return when {
        r1 <= l2 -> B(l1 + l2 - r1, r2)
        else -> B(l1, r2 + r1 - l2)
    }
}

/**
 * first in right bracket, second is left bracket
 * ie. inverted from what you'd imagine
 */
private fun parseBalanced(c: Char, p: PP): B {
    require(p.first != p.second) { "first and second can't be the same $p" }
    return when (c) {
        p.first -> B(0, 1)
        p.second -> B(1, 0)
        else -> B(0, 0)
    }
}

private fun addBalance(a: B, c: Char, p: PP): B =
        a + parseBalanced(c, p)

private fun addBalanceRound(a: B, c: Char) = addBalance(a, c, '(' to ')')
private fun addBalanceSquare(a: B, c: Char) = addBalance(a, c, '[' to ']')
private fun addBalanceCurly(a: B, c: Char) = addBalance(a, c, '[' to ']')

fun String.hasBalancedRoundBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceRound)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedSquareBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceSquare)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedCurlyBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceCurly)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedPairOf(p: PP): Result<B> {
    val res = this.fold(B(0, 0)) { a, n -> addBalance(a, n, p) }
    return (res == B(0, 0)) to res
}

val Result<*>.ok: Boolean get() = this.first
val Result<*>.nok: Boolean get() = !this.first
val <T> Result<T>.data: T get() = this.second

val Result<B>.left: Int get() = this.data.second
val Result<B>.right: Int get() = this.data.first
