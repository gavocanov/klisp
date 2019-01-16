@file:Suppress("unused")

package klisp.parser.derivative

import klisp.None
import klisp.Option
import klisp.Queue
import klisp.Some
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
open class LiveStream<A>(open val source: LiveStreamSource<A>) {
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

    private var headCache: Option<A> = None
    private var tailCache: Option<LiveStream<A>> = None

    /**
     * @return true if this object is currently the last element in a stream.
     */
    val isPlugged get() = headCache.isEmpty && !source.hasNext

    /**
     * @return true if this object is the last element in a stream, and the source is terminated.
     */
    val isEmpty get() = isPlugged && source.isTerminated

    /**
     * @return if not plugged, the object at this location in the stream.
     */
    val head: A
        get() = when (val hc = headCache) {
            is Some -> hc()
            is None -> {
                if (isPlugged)
                    throw IllegalStateException("can't pull a plugged head")
                val value = Some(source.next())
                headCache = value
                value()
            }
        }

    /**
     * @return if not plugged, the remainder of this stream.
     */
    val tail
        get() = if (isPlugged)
            throw IllegalStateException("can't pull a plugged tail")
        else when (val tc = tailCache) {
            is Some -> tc()
            is None -> {
                head
                val value = Some(LiveStream(source))
                tailCache = value
                value()
            }
        }

    override fun toString(): String = when {
        isEmpty -> "∅"
        isPlugged -> "◌"
        else -> "$head ː~ː $tail"
    }
}

data class LiveHT<A>(val head: A, val tail: LiveStream<A>?) {
    companion object {
        infix fun <A> unapply(ls: LiveStream<A>): Option<Pair<A, LiveStream<A>>> =
                if (ls.isPlugged) None
                else Some(ls.head to ls.tail)
    }
}

object LiveNil {
    infix fun <A> unapplySeq(ls: LiveStream<A>): Option<List<A>> =
            if (ls.isEmpty) Some(emptyList())
            else None
}

object LivePlug {
    infix fun <A> unapplySeq(ls: LiveStream<A>): Option<List<A>> =
            if (ls.isPlugged) Some(emptyList())
            else None
}
