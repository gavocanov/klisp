package klisp.expected

interface IQueue<T> {
    val isEmpty: Boolean
    fun dequeue(): T
    operator fun plusAssign(items: Iterable<T>)
    operator fun plusAssign(a: T)
}