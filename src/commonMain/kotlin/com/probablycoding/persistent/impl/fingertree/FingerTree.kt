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
package com.probablycoding.persistent.impl.fingertree

@Suppress("MemberVisibilityCanBePrivate", "unused")
class FingerTree<T, M> private constructor(private val measured: Measured<T, M>,
                                           private val front: Digit<T, M>,
                                           private val middle: FingerTree<Node<T, M>, M>?,
                                           private val back: Digit<T, M>) : Iterable<T> {
    private val measure: M by lazy {
        measured.sum(measured.sum(front.measure, middle?.measure ?: measured.zero), back.measure)
    }

    fun append(element: T): FingerTree<T, M> = when {
        isEmpty() -> FingerTree(measured, front.append(element), middle, back)
        back.hasRoom() -> FingerTree(measured, front, middle, back.append(element))
        else -> {
            val newMiddle = (middle ?: empty(measured.node())).append(back.head().toNode())
            FingerTree(measured, front, newMiddle, Digit.of(measured, back.last(), element))
        }
    }

    fun clear(): FingerTree<T, M> = empty(measured)

    fun descendingIterator(): Iterator<T> {
        val frontSeq = front.descendingIterator().asSequence()
        val backSeq = back.descendingIterator().asSequence()

        return if (middle == null) {
            (backSeq + frontSeq).iterator()
        } else {
            val middleSeq = middle.descendingIterator().asSequence().map {
                it.descendingIterator().asSequence()
            }.flatten()

            (backSeq + middleSeq + frontSeq).iterator()
        }
    }

    fun dropUntil(predicate: (M) -> Boolean): FingerTree<T, M> = split(predicate).second

    fun first(): T = front.first()

    fun head(): FingerTree<T, M> = createRight(front, middle, back.head())

    fun isEmpty(): Boolean = front.isEmpty() && middle == null

    fun isNotEmpty(): Boolean = !isEmpty()

    override fun iterator(): Iterator<T> {
        val frontSeq = front.asSequence()
        val backSeq = back.asSequence()

        return if (middle == null) {
            (frontSeq + backSeq).iterator()
        } else {
            val middleSeq = middle.asSequence().map { it.asSequence() }.flatten()
            (frontSeq + middleSeq + backSeq).iterator()
        }
    }

    fun last(): T = if (back.isEmpty()) front.last() else back.last()

    fun lookup(predicate: (M) -> Boolean): T? {
        val tree = dropUntil(predicate)
        return if (tree.isEmpty()) {
            null
        } else {
            tree.first()
        }
    }

    fun merge(other: FingerTree<T, M>): FingerTree<T, M> = when {
        isEmpty() -> other
        isSingle() -> other.prepend(first())
        other.isEmpty() -> this
        other.isSingle() -> append(other.first())
        else -> {
            val mergedMiddle = makeNodes(middle ?: empty(measured.node()), back, other.front)
            if (other.middle == null) {
                FingerTree(measured, front, mergedMiddle, other.back)
            } else {
                FingerTree(measured, front, mergedMiddle.merge(other.middle), other.back)
            }
        }
    }

    fun prepend(element: T): FingerTree<T, M> = when {
        isSingle() -> FingerTree(measured, Digit.of(measured, element), middle, front)
        front.hasRoom() -> FingerTree(measured, front.prepend(element), middle, back)
        else -> {
            val newMiddle = (middle ?: empty(measured.node())).append(front.tail().toNode())
            FingerTree(measured, Digit.of(measured, element, front.first()), newMiddle, back)
        }
    }

    fun reversed(): FingerTree<T, M> {
        val newFront = back.reversed()
        val newBack = front.reversed()

        return when (middle) {
            null -> FingerTree(measured, newFront, middle, newBack)
            else -> {
                val reversedMiddle = middle.reversed()
                val newMiddle = FingerTree(middle.measured, reversedMiddle.back, reversedMiddle.middle,
                        reversedMiddle.front)
                FingerTree(measured, newFront, newMiddle, newBack)
            }
        }
    }

    fun split(predicate: (M) -> Boolean): Pair<FingerTree<T, M>, FingerTree<T, M>> = when {
        isNotEmpty() && predicate(measure) -> {
            val split = splitTree(predicate)
            Pair(split.first, split.third.prepend(split.second))
        }
        else -> Pair(this, empty(measured))
    }

    fun splitTree(predicate: (M) -> Boolean, accumulator: M = measured.zero): Triple<FingerTree<T, M>, T, FingerTree<T, M>> = when {
        isSingle() -> Triple(empty(measured), front.first(), empty(measured))
        else -> {
            val frontAcc = measured.sum(accumulator, front.measure)
            if (predicate(frontAcc)) {
                val frontSplit = front.splitTree(predicate, accumulator)
                Triple(frontSplit.first.toTree(), frontSplit.second, create(frontSplit.third, middle, back))
            } else {
                val middleAcc = measured.sum(frontAcc, middle?.measure ?: measured.zero)
                if (middle != null && predicate(middleAcc)) {
                    val middleSplit = middle.splitTree(predicate, frontAcc)
                    val middleNodeAcc = measured.sum(frontAcc, middleSplit.first.measure)
                    val middleNodeSplit = middleSplit.second.splitTree(predicate, middleNodeAcc)
                    Triple(createRight(front, middleSplit.first, middleNodeSplit.first), middleNodeSplit.second,
                            create(middleNodeSplit.third, middleSplit.third, back))
                } else {
                    val backSplit = back.splitTree(predicate, middleAcc)
                    Triple(createRight(front, middle, backSplit.first), backSplit.second,
                            backSplit.third.toTree())
                }
            }
        }
    }

    fun tail(): FingerTree<T, M> = create(front.tail(), middle, back)

    fun takeUntil(predicate: (M) -> Boolean): FingerTree<T, M> = split(predicate).first

    private fun isSingle(): Boolean = middle == null && back.isEmpty()

    companion object {
        fun <T, M> empty(measured: Measured<T, M>): FingerTree<T, M> =
                FingerTree(measured, Digit.of(measured), null, Digit.of(measured))

        fun <T, M> fromSequence(measured: Measured<T, M>, sequence: Sequence<T>): FingerTree<T, M> =
                sequence.fold(empty(measured), FingerTree<T, M>::append)

        private fun <T, M> create(front: Digit<T, M>, middle: FingerTree<Node<T, M>, M>?, back: Digit<T, M>): FingerTree<T, M> {
            return if (front.isNotEmpty()) {
                FingerTree(front.measured, front, middle, back)
            } else if (middle == null || middle.isEmpty()) {
                fromSequence(front.measured, back.asSequence())
            } else {
                FingerTree(front.measured, middle.first().toDigit(), middle.tail(), back)
            }
        }

        private fun <T, M> createRight(front: Digit<T, M>, middle: FingerTree<Node<T, M>, M>?, back: Digit<T, M>): FingerTree<T, M> {
            return if (back.isNotEmpty()) {
                FingerTree(front.measured, front, middle, back)
            } else if (middle == null || middle.isEmpty()) {
                fromSequence(front.measured, front.asSequence())
            } else {
                FingerTree(front.measured, front, middle.head(), middle.last().toDigit())
            }
        }

        private fun <T, M> makeNodes(middle: FingerTree<Node<T, M>, M>, front: Digit<T, M>, back: Digit<T, M>): FingerTree<Node<T, M>, M> {
            val elements = (front.asSequence() + back.descendingIterator().asSequence()).toList()
            return when (elements.size) {
                2 -> middle.append(Node.of(front.measured, elements[0], elements[1]))
                3 -> middle.append(Node.of(front.measured, elements[0], elements[1], elements[2]))
                4 -> middle.append(Node.of(front.measured, elements[0], elements[1])).append(Node.of(front.measured, elements[2], elements[3]))
                5 -> middle.append(Node.of(front.measured, elements[0], elements[1], elements[2])).append(Node.of(front.measured, elements[3], elements[4]))
                6 -> middle.append(Node.of(front.measured, elements[0], elements[1], elements[2])).append(Node.of(front.measured, elements[3], elements[4], elements[5]))
                7 -> middle.append(Node.of(front.measured, elements[0], elements[1], elements[2])).append(Node.of(front.measured, elements[3], elements[4])).append(Node.of(front.measured, elements[5], elements[6]))
                8 -> middle.append(Node.of(front.measured, elements[0], elements[1], elements[2])).append(Node.of(front.measured, elements[3], elements[4], elements[5])).append(Node.of(front.measured, elements[6], elements[7]))
                else -> throw IllegalStateException("Nodes must have 2 or 3 elements")
            }
        }
    }
}
