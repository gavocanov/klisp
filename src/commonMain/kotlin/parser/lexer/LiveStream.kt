/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer

import klisp.None
import klisp.Option
import klisp.Some

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

    val toList by lazy {
        var (h, t) = head to tail
        val out = mutableListOf<A>()
        while (!t.isPlugged) {
            out.add(h)
            h = t.head
            t = t.tail
        }
        out.add(h)
        out
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
                val value = Some(source.next)
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