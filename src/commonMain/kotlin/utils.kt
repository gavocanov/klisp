package klisp

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