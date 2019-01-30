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

import com.probablycoding.persistent.ImmutableCollection
import com.probablycoding.persistent.ImmutableMap
import com.probablycoding.persistent.ImmutableSet

abstract class AbstractMap<K, V> : ImmutableMap<K, V> {
    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() = ImmutableEntrySet(this)
    override val keys: ImmutableSet<K>
        get() = ImmutableKeySet(this)
    override val values: ImmutableCollection<@UnsafeVariance V>
        get() = ImmutableValueCollection(this)

    override fun containsKey(key: K): Boolean = (this as Map<K, V>).any { it.key == key }

    override fun containsValue(value: @UnsafeVariance V): Boolean = (this as Map<K, V>).any { it.value == value }

    override operator fun get(key: K): V? = entries.firstOrNull { it.key == key }?.value

    override fun isEmpty(): Boolean = size == 0

    override fun putAll(from: Map<out K, V>): ImmutableMap<K, V> = from.entries.fold(this as ImmutableMap<K, V>) { map, entry -> map.put(entry.key, entry.value) }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Map<*, *>) return false

        if (size != other.size) return false

        return entries.all { other.containsKey(it.key) && other[it.key] == it.value }
    }

    override fun hashCode(): Int = entries.fold(0) { hash, entry -> hash + entry.hashCode() }

    override fun toString(): String = entries.joinToString(", ", "{", "}", -1, "...") { "${it.key}=${it.value}" }

    private class ImmutableEntrySet<K, V>(private val parent: ImmutableMap<K, V>) : AbstractSet<Map.Entry<K, V>>() {
        override val size = parent.size

        override fun contains(element: Map.Entry<K, V>): Boolean = parent.containsKey(element.key) && parent[element.key] == element.value

        override fun iterator(): Iterator<Map.Entry<K, V>> = parent.iterator()

        override fun add(element: Map.Entry<K, V>): ImmutableSet<Map.Entry<K, V>> = parent.put(element.key, element.value).entries

        override fun clear(): ImmutableSet<Map.Entry<K, V>> = parent.clear().entries
    }

    private class ImmutableKeySet<K>(private val parent: ImmutableMap<K, *>) : AbstractSet<K>() {
        override val size = parent.size

        override operator fun contains(element: K): Boolean = parent.containsKey(element)

        override operator fun iterator(): Iterator<K> {
            return object : Iterator<K> {
                private val iterator = parent.iterator()

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): K = iterator.next().key
            }
        }

        override fun add(element: K): ImmutableSet<K> {
            throw UnsupportedOperationException()
        }

        override fun clear(): ImmutableSet<K> = parent.clear().keys
    }

    private class ImmutableValueCollection<V>(private val parent: ImmutableMap<*, V>) : AbstractCollection<V>() {
        override val size = parent.size

        override operator fun contains(element: @UnsafeVariance V): Boolean = parent.containsValue(element)

        override operator fun iterator(): Iterator<@UnsafeVariance V> {
            return object : Iterator<V> {
                private val iterator = parent.iterator()

                override fun hasNext(): Boolean = iterator.hasNext()

                override fun next(): V = iterator.next().value
            }
        }

        override fun add(element: V): ImmutableCollection<V> {
            throw UnsupportedOperationException()
        }

        override fun clear(): ImmutableCollection<V> = parent.clear().values
    }
}
