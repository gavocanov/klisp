package klisp.expected

expect open class SortedSet<T : Comparable<T>>() : Set<T> {
    constructor(from: Collection<T>)
    constructor(from: T)
}