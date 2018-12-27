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

