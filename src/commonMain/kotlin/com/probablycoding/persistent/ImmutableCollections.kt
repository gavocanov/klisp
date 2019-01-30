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
@file:Suppress("unused")

package com.probablycoding.persistent

import com.probablycoding.persistent.impl.PersistentHashMap
import com.probablycoding.persistent.impl.PersistentHashSet
import com.probablycoding.persistent.impl.PersistentTreeMap
import com.probablycoding.persistent.impl.PersistentTreeSet
import com.probablycoding.persistent.impl.PersistentVector
import kotlin.reflect.KClass

// ImmutableCollection

/**
 * Adds all elements of the given [elements] collection to this [ImmutableCollection].
 */
fun <T> ImmutableCollection<T>.addAll(elements: Iterable<T>): ImmutableCollection<T> =
        if (elements is Collection && elements.isEmpty()) {
            this
        } else {
            addAll(elements.toList())
        }

/**
 * Adds all elements of the given [elements] sequence to this [ImmutableCollection].
 */
fun <T> ImmutableCollection<T>.addAll(elements: Sequence<T>): ImmutableCollection<T> = addAll(elements.toList())

/**
 * Adds all elements of the given [elements] array to this [ImmutableCollection].
 */
fun <T> ImmutableCollection<T>.addAll(elements: Array<out T>): ImmutableCollection<T> =
        if (elements.isEmpty()) {
            this
        } else {
            addAll(elements.asList())
        }

/**
 * Returns an immutable list containing all elements except first [count] elements.
 */
fun <T> ImmutableCollection<T>.drop(count: Int): ImmutableList<T> {
    require(count >= 0) { "Requested element count $count is less than zero." }
    val resultSize = size - count

    return when {
        count == 0 -> toImmutableList()
        resultSize <= 0 -> emptyImmutableList()
        resultSize == 1 -> immutableListOf(last())
        this is ImmutableList -> subList(count, size)
        else -> asSequence().drop(count).toImmutableList()
    }
}

/**
 * Returns an immutable list containing all elements except last [count] elements.
 */
fun <T> ImmutableCollection<T>.dropLast(count: Int): ImmutableList<T> {
    require(count >= 0) { "Requested element count $count is less than zero." }
    return take((size - count).coerceAtLeast(0))
}

/**
 * Returns an immutable list containing all elements except first elements that satisfy the given [predicate].
 */
inline fun <T> ImmutableCollection<T>.dropWhile(predicate: (T) -> Boolean): ImmutableList<T> {
    if (!isEmpty()) {
        for ((index, item) in withIndex()) {
            if (!predicate(item)) {
                return drop(index + 1)
            }
        }
    }

    return emptyImmutableList()
}

/**
 * Returns an immutable list containing only elements matching the given [predicate].
 */
inline fun <T> ImmutableCollection<T>.filter(crossinline predicate: (T) -> Boolean): ImmutableList<T> = asSequence().filter { predicate(it) }.toImmutableList()

/**
 * Returns an immutable list containing only elements matching the given [predicate].
 * @param [predicate] function that takes the index of an element and the element itself
 * and returns the result of predicate evaluation on the element.
 */
inline fun <T> ImmutableCollection<T>.filterIndexed(crossinline predicate: (Int, T) -> Boolean): ImmutableList<T> = asSequence().filterIndexed { index, element -> predicate(index, element) }.toImmutableList()

/**
 * Returns an immutable list containing all elements that are instances of specified type parameter R.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified R> ImmutableCollection<*>.filterIsInstance(): ImmutableList<R> = filter { it is R } as ImmutableList<R>

/**
 * Returns an immutable list containing all elements that are instances of specified class.
 */
@Suppress("UNCHECKED_CAST")
fun <R : Any> ImmutableCollection<*>.filterIsInstance(clazz: KClass<R>): ImmutableList<R> = filter { clazz.isInstance(it) } as ImmutableList<R>

/**
 * Returns an immutable list containing all elements not matching the given [predicate].
 */
inline fun <T> ImmutableCollection<T>.filterNot(crossinline predicate: (T) -> Boolean): ImmutableList<T> = filter { !predicate(it) }

/**
 * Returns an immutable list containing all elements that are not `null`.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> ImmutableCollection<T?>.filterNotNull(): ImmutableList<T> = filter { it != null } as ImmutableList<T>

/**
 * Returns a single immutable list of all elements yielded from results of
 * [transform] function being invoked on each element of original collection.
 */
inline fun <T, R> ImmutableCollection<T>.flatMap(crossinline transform: (T) -> Iterable<R>): ImmutableList<R> = asSequence().flatMap { transform(it).asSequence() }.toImmutableList()

/**
 * Returns an immutable set containing all elements that are contained by both
 * this collection and the specified collection.
 * The returned set preserves the element iteration order of the original
 * collection.
 */
infix fun <T> ImmutableCollection<T>.intersect(other: Iterable<T>): ImmutableSet<T> =// TODO: This doesn't preserve iteration order, need an ordered set
        asSequence().filterNot { it in other }.toImmutableSet()

/**
 * Returns an immutable collection containing all elements of the original
 * collection without the first occurrence of the given [element].
 */
operator fun <T> ImmutableCollection<T>.minus(element: T): ImmutableCollection<T> = remove(element)

/**
 * Returns an immutable collection containing all elements of the original
 * collection except the elements contained in the given [elements] array.
 */
operator fun <T> ImmutableCollection<T>.minus(elements: Array<out T>): ImmutableCollection<T> = removeAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection except the elements contained in the given [elements] collection.
 */
operator fun <T> ImmutableCollection<T>.minus(elements: Iterable<T>): ImmutableCollection<T> = removeAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection except the elements contained in the given [elements] sequence.
 */
operator fun <T> ImmutableCollection<T>.minus(elements: Sequence<T>): ImmutableCollection<T> = removeAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection without the first occurrence of the given [element].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> ImmutableCollection<T>.minusElement(element: T): ImmutableCollection<T> = minus(element)

/**
 * Returns an immutable collection containing all elements of the original
 * collection and then the given [element].
 */
operator fun <T> ImmutableCollection<T>.plus(element: T): ImmutableCollection<T> = add(element)

/**
 * Returns an immutable collection containing all elements of the original
 * collection and then all elements of the given [elements] array.
 */
operator fun <T> ImmutableCollection<T>.plus(elements: Array<out T>): ImmutableCollection<T> = addAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection and then all elements of the given [elements] collection.
 */
operator fun <T> ImmutableCollection<T>.plus(elements: Iterable<T>): ImmutableCollection<T> = addAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection and then all elements of the given [elements] sequence.
 */
operator fun <T> ImmutableCollection<T>.plus(elements: Sequence<T>): ImmutableCollection<T> = addAll(elements)

/**
 * Returns an immutable collection containing all elements of the original
 * collection and then the given [element].
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <T> ImmutableCollection<T>.plusElement(element: T): ImmutableCollection<T> = plus(element)

/**
 * Removes all elements from this [ImmutableCollection] that are also contained
 * in the given [elements] collection.
 */
fun <T> ImmutableCollection<T>.removeAll(elements: Iterable<T>): ImmutableCollection<T> =
        if (elements is Collection && elements.isEmpty()) {
            this
        } else {
            removeAll(elements.toList())
        }

/**
 * Removes all elements from this [ImmutableCollection] that are also contained
 * in the given [elements] sequence.
 */
fun <T> ImmutableCollection<T>.removeAll(elements: Sequence<T>): ImmutableCollection<T> {
    val other = elements.toHashSet()
    return if (other.isEmpty()) {
        this
    } else {
        removeAll(other)
    }
}

/**
 * Removes all elements from this [MutableCollection] that are also contained in the given [elements] array.
 */
fun <T> ImmutableCollection<T>.removeAll(elements: Array<out T>): ImmutableCollection<T> =
        if (elements.isEmpty()) {
            this
        } else {
            removeAll(elements.asList())
        }

/**
 * Retains only elements of this [ImmutableCollection] that are contained in
 * the given [elements] collection.
 */
fun <T> ImmutableCollection<T>.retainAll(elements: Iterable<T>): ImmutableCollection<T> =
        if (elements is Collection && elements.isEmpty()) {
            clear()
        } else {
            retainAll(elements.toHashSet())
        }

/**
 * Retains only elements of this [ImmutableCollection] that are contained in
 * the given [elements] array.
 */
fun <T> ImmutableCollection<T>.retainAll(elements: Array<out T>): ImmutableCollection<T> =
        if (elements.isEmpty()) {
            clear()
        } else {
            retainAll(elements.asList())
        }

/**
 * Retains only elements of this [ImmutableCollection] that are contained in
 * the given [elements] sequence.
 */
fun <T> ImmutableCollection<T>.retainAll(elements: Sequence<T>): ImmutableCollection<T> {
    val other = elements.toHashSet()
    return if (other.isEmpty()) {
        clear()
    } else {
        retainAll(other)
    }
}

/**
 * Returns an immutable list with elements in reversed order.
 */
fun <T> ImmutableCollection<T>.reversed(): ImmutableList<T> = when {
    isEmpty() -> emptyImmutableList()
    this is ImmutableList -> reversed()
    else -> toList().asReversed().toImmutableList()
}

/**
 * Returns an immutable list of all elements sorted according to their natural sort order.
 */
fun <T : Comparable<T>> ImmutableCollection<T>.sorted(): ImmutableList<T> =
        if (isEmpty()) {
            emptyImmutableList()
        } else {
            toMutableList().apply { sort() }.toImmutableList()
        }

/**
 * Returns an immutable list of all elements sorted according to natural sort order of the value returned by specified [selector] function.
 */
inline fun <T, R : Comparable<R>> ImmutableCollection<T>.sortedBy(crossinline selector: (T) -> R?): ImmutableList<T> = sortedWith(compareBy(selector))

/**
 * Returns an immutable list of all elements sorted descending according to natural sort order of the value returned by specified [selector] function.
 */
inline fun <T, R : Comparable<R>> ImmutableCollection<T>.sortedByDescending(crossinline selector: (T) -> R?): ImmutableList<T> = sortedWith(compareByDescending(selector))

/**
 * Returns an immutable list of all elements sorted descending according to their natural sort order.
 */
fun <T : Comparable<T>> ImmutableCollection<T>.sortedDescending(): ImmutableList<T> = sortedWith(reverseOrder())

/**
 * Returns an immutable list of all elements sorted according to the specified [comparator].
 */
fun <T> ImmutableCollection<T>.sortedWith(comparator: Comparator<in T>): ImmutableList<T> =
        if (isEmpty()) {
            emptyImmutableList()
        } else {
            toMutableList().apply { sortWith(comparator) }.toImmutableList()
        }

/**
 * Returns an immutable set containing all elements that are contained by this
 * collection and not contained by the specified collection.
 * The returned set preserves the element iteration order of the original
 * collection.
 */
infix fun <T> ImmutableCollection<T>.subtract(other: Iterable<T>): Set<T> =// TODO: This doesn't preserve iteration order, need an ordered set
        (asSequence() - other.asSequence()).toImmutableSet()

/**
 * Returns an immutable list containing first [count] elements.
 */
fun <T> ImmutableCollection<T>.take(count: Int): ImmutableList<T> {
    require(count >= 0) { "Requested element count $count is less than zero." }

    return when {
        count == 0 -> emptyImmutableList()
        count >= size -> toImmutableList()
        count == 1 -> immutableListOf(first())
        this is ImmutableList -> subList(0, count)
        else -> asSequence().take(count).toImmutableList()
    }
}

/**
 * Returns an immutable list containing last [count] elements.
 */
fun <T> ImmutableCollection<T>.takeLast(count: Int): ImmutableList<T> {
    require(count >= 0) { "Requested element count $count is less than zero." }
    return drop((size - count).coerceAtLeast(0))
}

/**
 * Returns an immutable list containing first elements satisfying the given [predicate].
 */
inline fun <T> ImmutableCollection<T>.takeWhile(predicate: (T) -> Boolean): ImmutableList<T> {
    if (!isEmpty()) {
        for ((index, item) in withIndex()) {
            if (!predicate(item)) {
                return take(index - 1)
            }
        }
    }

    return emptyImmutableList()
}

/**
 * Returns a set containing all distinct elements from both collections.
 * The returned set preserves the element iteration order of the original
 * collection.
 * Those elements of the [other] collection that are unique are iterated in the
 * end in the order of the [other] collection.
 */
infix fun <T> ImmutableCollection<T>.union(other: Iterable<T>): ImmutableSet<T> =// TODO: This doesn't preserve iteration order, need an ordered set
        (asSequence() + other.asSequence()).toImmutableSet()

// ImmutableList

/**
 * Returns an immutable list containing all elements except last elements that satisfy the given [predicate].
 */
inline fun <T> ImmutableList<T>.dropLastWhile(predicate: (T) -> Boolean): ImmutableList<T> {
    if (!isEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (!predicate(iterator.previous())) {
                return take(iterator.nextIndex() + 1)
            }
        }
    }

    return emptyImmutableList()
}

/**
 * Returns an immutable list containing elements at indices in the specified [indices] range.
 */
fun <T> ImmutableList<T>.slice(indices: IntRange): ImmutableList<T> =
        if (indices.isEmpty()) {
            emptyImmutableList()
        } else {
            subList(indices.start, indices.endInclusive + 1)
        }

/**
 * Returns an immutable list containing elements at specified [indices].
 */
fun <T> ImmutableList<T>.slice(indices: Iterable<Int>): ImmutableList<T> {
    val size = if (indices is Collection) indices.size else 10
    return if (size == 0) {
        emptyImmutableList()
    } else {
        // Could skip this if transients were exposed to the API...
        val list = ArrayList<T>(size)
        indices.forEach { list.add(get(it)) }
        list.toImmutableList()
    }
}

/**
 * Returns an immutable list containing last elements satisfying the given [predicate].
 */
inline fun <T> ImmutableList<T>.takeLastWhile(predicate: (T) -> Boolean): ImmutableList<T> {
    if (!isEmpty()) {
        val iterator = listIterator(size)
        while (iterator.hasPrevious()) {
            if (!predicate(iterator.previous())) {
                return drop(iterator.nextIndex() + 1)
            }
        }
    }

    return emptyImmutableList()
}

// Iterable

/**
 * Returns an [ImmutableMap] containing key-value pairs provided by [transform]
 * function applied to elements of the given collection.
 * If any of two pairs would have the same key the last one gets added to the map.
 * The returned map preserves the entry iteration order of the original collection.
 */
inline fun <T, K, V> Iterable<T>.associateImmutable(crossinline transform: (T) -> Pair<K, V>): ImmutableMap<K, V> =// TODO: This doesn't preserve iteration order, need an ordered map
        asSequence().associateImmutable { transform(it) }

/**
 * Returns an [ImmutableMap] containing the elements from the given collection indexed by the key
 * returned from [keySelector] function applied to each element.
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 * The returned map preserves the entry iteration order of the original collection.
 */
inline fun <T, K> Iterable<T>.associateImmutableBy(crossinline keySelector: (T) -> K): ImmutableMap<K, T> =// TODO: This doesn't preserve iteration order, need an ordered map
        asSequence().associateImmutableBy { keySelector(it) }

/**
 * Returns an [ImmutableMap] containing the values provided by [valueTransform] and indexed by [keySelector] functions applied to elements of the given collection.
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the map.
 * The returned map preserves the entry iteration order of the original collection.
 */
fun <T, K, V> Iterable<T>.associateImmutableBy(keySelector: (T) -> K, valueTransform: (T) -> V): ImmutableMap<K, V> =// TODO: This doesn't preserve iteration order, need an ordered map
        asSequence().associateImmutableBy(keySelector, valueTransform)

/**
 * Returns an [ImmutableList] containing all elements.
 */
fun <T> Iterable<T>.toImmutableList(): ImmutableList<T> =
        if (this is Collection && isEmpty()) {
            emptyImmutableList()
        } else {
            asSequence().toImmutableList()
        }

/**
 * Returns an [ImmutableSet] of all elements.
 * The returned set preserves the element iteration order of the original collection.
 */
fun <T> Iterable<T>.toImmutableSet(): ImmutableSet<T> =
        if (this is Collection && isEmpty()) {
            emptyImmutableSet()
        } else {
            // TODO: This doesn't preserve iteration order, need an ordered set
            asSequence().toImmutableSet()
        }

/**
 * Returns an [ImmutableSortedSet] of all elements.
 */
fun <T : Comparable<T>> Iterable<T>.toImmutableSortedSet(): ImmutableSortedSet<T> = asSequence().toImmutableSortedSet()

// Free standing

fun <T> emptyImmutableList(): ImmutableList<T> = PersistentVector.empty()

fun <K, V> emptyImmutableMap(): ImmutableMap<K, V> = PersistentHashMap.empty()

fun <T> emptyImmutableSet(): ImmutableSet<T> = PersistentHashSet.empty()

fun <K : Comparable<K>, V> emptyImmutableSortedMap(): ImmutableSortedMap<K, V> = PersistentTreeMap.empty()

fun <K, V> emptyImmutableSortedMap(comparator: Comparator<in K>): ImmutableSortedMap<K, V> = PersistentTreeMap.empty(comparator)

fun <T : Comparable<T>> emptyImmutableSortedSet(): ImmutableSortedSet<T> = PersistentTreeSet.empty()

fun <T> emptyImmutableSortedSet(comparator: Comparator<in T>): ImmutableSortedSet<T> = PersistentTreeSet.empty(comparator)

fun <T> immutableListOf(vararg elements: T): ImmutableList<T> =
        if (elements.isEmpty()) {
            emptyImmutableList()
        } else {
            PersistentVector.of(*elements)
        }

fun <K, V> immutableMapOf(vararg pairs: Pair<K, V>): ImmutableMap<K, V> =
        if (pairs.isEmpty()) {
            emptyImmutableMap()
        } else {
            PersistentHashMap.of(*pairs)
        }

fun <T> immutableSetOf(vararg elements: T): ImmutableSet<T> =
        if (elements.isEmpty()) {
            emptyImmutableSet()
        } else {
            PersistentHashSet.of(*elements)
        }

fun <K : Comparable<K>, V> immutableSortedMapOf(vararg pairs: Pair<K, V>): ImmutableSortedMap<K, V> =
        if (pairs.isEmpty()) {
            emptyImmutableSortedMap()
        } else {
            PersistentTreeMap.of(*pairs)
        }

fun <K, V> immutableSortedMapOf(comparator: Comparator<in K>, vararg pairs: Pair<K, V>): ImmutableSortedMap<K, V> =
        if (pairs.isEmpty()) {
            emptyImmutableSortedMap(comparator)
        } else {
            PersistentTreeMap.of(comparator, *pairs)
        }

fun <T : Comparable<T>> immutableSortedSetOf(vararg elements: T): ImmutableSortedSet<T> =
        if (elements.isEmpty()) {
            emptyImmutableSortedSet()
        } else {
            PersistentTreeSet.of(*elements)
        }

fun <T> immutableSortedSetOf(comparator: Comparator<in T>, vararg elements: T): ImmutableSortedSet<T> =
        if (elements.isEmpty()) {
            emptyImmutableSortedSet(comparator)
        } else {
            PersistentTreeSet.of(comparator, *elements)
        }
