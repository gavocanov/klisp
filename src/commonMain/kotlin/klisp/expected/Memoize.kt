package klisp.expected

expect class Memoize<I, O>(backingMap: MutableMap<I, O>, fn: (I) -> O) : (I) -> O