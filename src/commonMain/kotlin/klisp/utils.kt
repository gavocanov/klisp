@file:Suppress("unused")

package klisp

import klisp.expected.Platform
import klisp.tty.TermColors
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic

fun took(_start: Long) = "took ${Platform.strFormat(((Platform.getTimeNanos() - _start) / 1e6))} ms"

fun tryOrNil(f: () -> exp) = try {
    f()
} catch (_: Throwable) {
    nil
}

typealias Result<T> = Pair<Boolean, T>

val Result<*>.ok: Boolean get() = first
val Result<*>.nok: Boolean get() = !first
val <T> Result<T>.data: T get() = second

val <T> List<T>.reversed: List<T> get() = this.asReversed()

infix fun <T> Set<T>.subsetOf(other: Set<T>) = other.containsAll(this)

inline val <T> T?.exIfNull: T
    get() = this ?: throw NullPointerException()

inline val <T> List<T>.first: T
    get() =
        if (isEmpty()) throw NoSuchElementException()
        else this[0]

inline val <T> List<T>.last: T
    get() =
        if (isEmpty()) throw NoSuchElementException()
        else this[size - 1]

inline val <T> List<T>.rest: List<T>
    get() = when {
        isEmpty() -> throw NoSuchElementException()
        size > 1 -> subList(1, size)
        else -> emptyList()
    }

inline val <T> List<T>.head: T get() = first
inline val <T> List<T>.tail: List<T> get() = rest

inline infix fun <reified T> List<T>.unApply(n: Int): List<Any?> {
    require(n > 1) { "number must be bigger then 1" }
    val hd = when {
        n <= size -> subList(0, n - 1)
        n == size -> this
        else -> (0 until n - 1).map { this.getOrNull(it) }
    }

    val tl =
            if (n <= size)
                subList(n - 1, size)
            else
                emptyList()

    return listOf(*hd.toTypedArray(), tl)
}

inline val String.first: Char get() = if (isEmpty()) throw NoSuchElementException() else this[0]
inline val String.last: Char get() = if (isEmpty()) throw NoSuchElementException() else this[length - 1]
inline val String.head: Char get() = first
inline val String.tail: String get() = rest
inline val String.rest: String
    get() = when {
        isEmpty() -> throw NoSuchElementException()
        length > 1 -> substring(1)
        else -> ""
    }

val <T> T.singletonList: List<T> get() = listOf(this)
infix fun <T> T.cons(l: Iterable<T>): List<T> = listOf(this) + l
infix fun <T> T.cons(e: T): List<T> = listOf(this, e)
infix fun <T> List<T>.cons(e: T): List<T> = this + e
infix fun <T> List<T>.cons(l: Iterable<T>): List<T> = this + l

/**
 * split by space not contained in "", '', [] and {}
 */
fun splitNotSurrounded(s: String): List<String> =
        "[^\\s\"'{\\[]+|\"([^\"]*)\"|'([^']*)'|\\{([^{]*)}|\\[([^\\[]*)]"
                .toRegex()
                .findAll(s)
                .map { it.value }
                .toList()

object LOGGER {
    private val C = TermColors()
    fun trace(msg: Any?) = println(C.gray(msg.toString()))
    fun debug(msg: Any?) = println(C.yellow(msg.toString()))
    fun info(msg: Any?) = println(C.brightBlue(msg.toString()))
    fun warn(msg: Any?) = println(C.brightYellow(msg.toString()))
    fun error(msg: Any?) = println(C.brightRed(msg.toString()))
}

class ChainMap<K, V>(private val map: MutableMap<K, V>) : MutableMap<K, V> {
    private val innerMap = mutableMapOf<K, V>()

    // those are just proxies and should not be used, here for completeness

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = (innerMap.entries + map.entries).toMutableSet()
    override val keys: MutableSet<K> get() = (innerMap.keys + map.keys).toMutableSet()
    override val values: MutableCollection<V> get() = (innerMap.values + map.values).toMutableList()

    // from here on we deal with the stuff

    override val size: Int = innerMap.size + map.size
    override fun clear() = innerMap.clear()
    override infix fun containsKey(key: K): Boolean = innerMap.containsKey(key) || map.containsKey(key)
    override infix fun containsValue(value: V): Boolean = innerMap.containsValue(value) || map.containsValue(value)
    override infix fun get(key: K): V? = innerMap[key] ?: map[key]
    override fun isEmpty(): Boolean = innerMap.isEmpty() && map.isEmpty()
    override fun put(key: K, value: V): V? = innerMap.put(key, value)
    override infix fun putAll(from: Map<out K, V>) = innerMap.putAll(from)
    override infix fun remove(key: K): V? = innerMap.remove(key)
}

/**
 * Minimal option type for Kotlin
 * from https://github.com/gojuno/koptional/blob/master/koptional/src/main/kotlin/com/gojuno/koptional/Optional.kt
 * This implementation supports even Some(null)
 */
sealed class Option<out T> : Iterable<T> {
    /**
     * Converts [Option] to either its non-null value if it's [Some] or `null` if it's [None].
     */
    abstract fun toNullable(): T?

    /**
     * Unwraps this optional into the value it holds or null if there is no value held.
     */
    @JvmSynthetic
    abstract operator fun component1(): T?

    /**
     * Returns true if the option is [None], false otherwise.
     */
    abstract val isEmpty: Boolean

    companion object {
        /**
         * Wraps an instance of T (or null) into an [Option]:
         *
         * ```java
         * String a = "str";
         * String b = null;
         *
         * Option<String> optionalA = Option.toOptional(a); // Some("str")
         * Option<String> optionalB = Option.toOptional(b); // None
         * ```
         *
         * This is the preferred method of obtaining an instance of [Option] in Java. In Kotlin,
         * prefer using the [toOptional][klisp.toOptional] extension function.
         */
        @JvmStatic
        fun <T> toOptional(value: T?): Option<T> = if (value == null) None else Some(value)
    }
}

data class Some<out T>(private val value: T) : Option<T>() {
    override fun iterator(): Iterator<T> = listOf(value).iterator()
    override val isEmpty: Boolean = value === null
    override fun toString() = "Some($value)"
    override fun toNullable(): T? = value
    operator fun invoke(): T = value.exIfNull
    fun get(): T = invoke()
}

object None : Option<Nothing>() {
    override fun iterator(): Iterator<Nothing> = emptyList<Nothing>().iterator()
    override val isEmpty: Boolean = true
    override fun toString() = "None"
    override operator fun component1(): Nothing? = null
    override fun toNullable(): Nothing? = null
}

/**
 * Wraps an instance of [T] (or null) into an [Option]:
 *
 * ```kotlin
 * val a: String? = "str"
 * val b: String? = null
 *
 * val optionalA = a.toOptional() // Some("str")
 * val optionalB = b.toOptional() // None
 * ```
 *
 * This is the preferred method of obtaining an instance of [Option] in Kotlin. In Java, prefer
 * using the static [Option.toOptional] method.
 */
fun <T> T?.toOptional(): Option<T> =
        if (this == null) None
        else Some(this)

/**
 * Kind of pattern matching
 */
data class Case<in O, out N>(val matchFunc: (O) -> Boolean,
                             val result: (O) -> N?,
                             val isDefault: Boolean = false)

fun <T, N> T.match(vararg cases: Case<T, N>): N? {
    val matchingCase = cases
            .filter { (matchFunc) -> matchFunc(this) }
            .sortedBy { c -> c.isDefault }
            .firstOrNull()
    if (matchingCase == null) {
        throw RuntimeException("no match found")
    } else {
        return matchingCase.result(this)
    }
}

fun <O, N> case(matchFunc: (O) -> Boolean,
                result: (O) -> N)
        : Case<O, N> = Case(matchFunc, result)

fun <O, N> wildcard(result: (O) -> N)
        : Case<O, N> = Case({ _ -> true }, result, true)

