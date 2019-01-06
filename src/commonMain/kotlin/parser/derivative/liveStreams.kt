@file:Suppress("unused")

package klisp.parser.derivative

import klisp.Queue
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
    fun next(): A = queue.dequeue()

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

/**
 * A live stream is a stream whose tail may grow over time.
 * Every stream has a source which determines its tail.
 */
data class LiveStream<A>(val source: LiveStreamSource<A>) {
    companion object {
        operator fun invoke(s: String): LiveStream<Char> {
            val src = LiveStreamSource<Char>()
            src += s.toList()
            src.terminate()
            return LiveStream(src)
        }

        operator fun <T> invoke(it: Iterable<T>): LiveStream<T> {
            val src = LiveStreamSource<T>()
            src += it
            src.terminate()
            return LiveStream(src)
        }
    }

    private var headCache: A? = null
    private var tailCache: LiveStream<A>? = null

    /**
     * @return true if this object is currently the last element in a stream.
     */
    val isPlugged get() = headCache === null && !source.hasNext

    /**
     * @return true if this object is the last element in a stream, and the source is terminated.
     */
    val isEmpty get() = isPlugged && source.isTerminated

    /**
     * @return if not plugged, the object at this location in the stream.
     */
    val head: A
        get() = if (headCache !== null)
            headCache ?: throw NullPointerException()
        else {
            if (isPlugged) throw IllegalStateException("can't pull a plugged head")
            headCache = source.next() ?: throw NullPointerException()
            headCache ?: throw NullPointerException()
        }

    /**
     * @return if not plugged, the remainder of this stream.
     */
    val tail
        get() = if (isPlugged)
            throw IllegalStateException("can't pull a plugged tail")
        else {
            if (tailCache !== null)
                tailCache ?: throw NullPointerException()
            else {
                this.head
                tailCache = LiveStream(source)
                tailCache ?: throw NullPointerException()
            }
        }

    override fun toString(): String = when {
        isEmpty -> "∅"
        isPlugged -> "◌"
        else -> "$head ː~ː $tail"
    }
}

