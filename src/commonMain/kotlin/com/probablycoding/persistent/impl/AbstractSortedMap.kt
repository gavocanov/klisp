/*
 * Copyright (C) 2016 - Travis Watkins <amaranth@probablycoding.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.probablycoding.persistent.impl

import com.probablycoding.persistent.ImmutableSortedMap
import com.probablycoding.persistent.ImmutableSortedSet

abstract class AbstractSortedMap<K, V> : AbstractMap<K, V>(), ImmutableSortedMap<K, V> {
    abstract val comparator: Comparator<in K>
    override val entries: ImmutableSortedSet<Map.Entry<K, V>>
        get() = ImmutableSortedEntrySet(this)
    override val keys: ImmutableSortedSet<K>
        get() = ImmutableSortedKeySet(this)

    override fun putAll(from: Map<out K, V>): ImmutableSortedMap<K, V> = putAll(from)

    override fun firstKey(): K = firstEntry().key

    override fun firstValue(): V = firstEntry().value

    override fun headMap(toKey: K): ImmutableSortedMap<K, V> =
            if (comparator.compare(firstKey(), toKey) >= 0) {
                clear()
            } else {
                subMap(firstKey(), toKey)
            }

    override fun lastKey(): K = lastEntry().key

    override fun lastValue(): V = lastEntry().value

    private class ImmutableSortedEntrySet<K, V>(private val parent: AbstractSortedMap<K, V>) : AbstractSortedSet<Map.Entry<K, V>>() {
        override val comparator: Comparator<Map.Entry<K, V>> = Comparator { first, second -> parent.comparator.compare(first.key, second.key) }
        override val size = parent.size

        override operator fun contains(element: Map.Entry<K, V>): Boolean = parent.containsKey(element.key) && parent[element.key] == element.value

        override operator fun iterator(): Iterator<Map.Entry<K, V>> = parent.iterator()

        override fun comparator(): Comparator<in Map.Entry<K, V>>? {
            val comparator = parent.comparator()
            return if (comparator == null) {
                null
            } else {
                this.comparator
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun first(): Map.Entry<K, V> {
            val key = parent.firstKey()
            return Entry(key, parent[key] as V)
        }

        @Suppress("UNCHECKED_CAST")
        override fun last(): Map.Entry<K, V> {
            val key = parent.lastKey()
            return Entry(key, parent[key] as V)
        }

        override fun subSet(fromElement: Map.Entry<K, V>, toElement: Map.Entry<K, V>): ImmutableSortedSet<Map.Entry<K, V>> = parent.subMap(fromElement.key, toElement.key).entries

        override fun tailSet(fromElement: Map.Entry<K, V>): ImmutableSortedSet<Map.Entry<K, V>> = parent.tailMap(fromElement.key).entries

        override fun add(element: Map.Entry<K, V>): ImmutableSortedSet<Map.Entry<K, V>> = parent.put(element.key, element.value).entries

        override fun clear(): ImmutableSortedSet<Map.Entry<K, V>> = parent.clear().entries
    }

    private class ImmutableSortedKeySet<K>(private val parent: AbstractSortedMap<K, *>) : AbstractSortedSet<K>() {
        override val comparator = parent.comparator
        override val size = parent.size

        override operator fun contains(element: K): Boolean = parent.containsKey(element)

        override operator fun iterator(): Iterator<K> {
            return object : Iterator<K> {
                private val iterator = parent.iterator()

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): K = iterator.next().key
            }
        }

        override fun comparator(): Comparator<in K>? = parent.comparator()

        override fun first(): K = parent.firstKey()

        override fun last(): K = parent.lastKey()

        override fun subSet(fromElement: K, toElement: K): ImmutableSortedSet<K> = parent.subMap(fromElement, toElement).keys

        override fun tailSet(fromElement: K): ImmutableSortedSet<K> = parent.tailMap(fromElement).keys

        override fun add(element: K): ImmutableSortedSet<K> {
            throw UnsupportedOperationException()
        }

        override fun clear(): ImmutableSortedSet<K> = parent.clear().keys
    }
}
