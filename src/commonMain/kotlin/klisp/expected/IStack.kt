package klisp.expected

interface IStack<T> {
    fun push(e: T)
    fun pop(): T
    fun isNotEmpty(): Boolean
}