package klisp.expected

interface IFixedPoint {
    fun stabilized(): Boolean
    fun running(): Boolean
    fun changed(): Boolean
    fun generation(): Int
    fun master(): Any?

    fun stabilized(v: Boolean)
    fun running(v: Boolean)
    fun changed(v: Boolean)
    fun incGeneration()
    fun master(v: Any?)

    fun reset()
}