/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer

import klisp.expected.Queue
import klisp.cons

/**
 * Provides the input to a live stream.
 */
class LiveStreamSource<A> {
    private var _isTerminated = false
    private val queue = Queue<A>()
    private var listeners: List<(List<A>) -> Unit> = emptyList()

    /**
     * @return true if this source has been terminated.
     */
    val isTerminated get() = _isTerminated

    /**
     * @return true if this source will never have more input.
     */
    val isEmpty get() = queue.isEmpty && isTerminated

    /**
     * @return true if this source has more input ready.
     */
    val hasNext get() = !queue.isEmpty

    /**
     * @return the next input.
     */
    val next: A get() = queue.dequeue()

    /**
     * Adds another element to this source.
     */
    operator fun plusAssign(items: Iterable<A>) {
        queue += items
        listeners.forEach { l -> l(items.toList()) }
    }

    /**
     * Adds several more elements to this source.
     */
    operator fun plusAssign(item: A) {
        queue += item
        listeners.forEach { l -> l(listOf(item)) }
    }

    /**
     * Adds a listener to this source; gets called whenever new
     * elements are added.
     */
    fun addListener(l: (List<A>) -> Unit) {
        listeners = l cons listeners
    }

    /**
     * Terminates this source.
     */
    fun terminate() {
        _isTerminated = true
        listeners.forEach { l -> l(emptyList()) }
    }
}