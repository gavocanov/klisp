@file:Suppress("unused")

package klisp

import klisp.tty.TermColors

fun took(_start: Long) = "took ${Platform.strFormat(((Platform.getTimeNanos() - _start) / 1e6))} ms"

fun tryOrNil(f: () -> exp) = try {
    f()
} catch (_: Throwable) {
    nil
}

typealias Result<T> = Pair<Boolean, T>

val Result<*>.ok: Boolean get() = first
val Result<*>.nok: Boolean get() = !first
val <T> Result<T>.data: T get() = second

val <T> List<T>.reversed: List<T> get() = this.asReversed()

infix fun <T> Set<T>.subsetOf(other: Set<T>) = other.containsAll(this)

val <T> Iterable<T>.first: T get() = first()
val <T> Iterable<T>.last: T get() = last()
val <T> Iterable<T>.rest: List<T> get() = drop(1)
val <T> Iterable<T>.head: T get() = first()
val <T> Iterable<T>.tail: List<T> get() = drop(1)

val String.first: Char get() = first()
val String.last: Char get() = last()
val String.rest: String get() = substring(1)
val String.head: Char get() = first()
val String.tail: String get() = substring(1)

fun <T> T.toListOf(): List<T> = listOf(this)
infix fun <T> T.cons(list: Iterable<T>): List<T> = listOf(this) + list.asSequence()

fun <T : Any, R : Any> T?.nullableMap(mapping: (T) -> R): R? = when {
    this == null -> null
    else -> mapping(this)
}

fun <T : Any> T?.nullableFilter(predicate: (T) -> Boolean): T? = when {
    this == null -> null
    predicate(this) -> this
    else -> null
}
/**
 * split by space not contained in "", '', [] and {}
 */
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

