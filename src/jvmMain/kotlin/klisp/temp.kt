package klisp

interface IPQueue<T> {
    val isEmpty: Boolean
    val isNotEmpty: Boolean get() = !isEmpty
    fun dequeue(): IPQueue<T>
    fun peek(): T
    fun put(element: T): IPQueue<T>
}

abstract class PersistentQueue<T> : IPQueue<T> {
    companion object {
        fun <T> emptyQueue(): IPQueue<T> = object : PersistentQueue<T>() {
            override val isEmpty: Boolean get() = true
            override fun dequeue(): IPQueue<T> = throw NoSuchElementException()
            override fun peek(): T = throw NoSuchElementException()
            override fun toString(): String = "Empty"
        }
    }

    abstract override fun dequeue(): IPQueue<T>
    abstract override fun peek(): T
    abstract override val isEmpty: Boolean

    protected open val list: List<T> = emptyList()

    override fun put(element: T): PersistentQueue<T> = LinkNode(list, element)

    inner class LinkNode : PersistentQueue<T> {
        constructor(previous: List<T>, element: T) : super() {
            this.list = previous + element
        }

        constructor(previous: List<T>) : super() {
            this.list = previous
        }

        override val list: List<T>
        override val isEmpty: Boolean get() = list.isEmpty()

        override fun dequeue(): IPQueue<T> = LinkNode(list.tail)
        override fun peek(): T = if (list.isEmpty()) throw NoSuchElementException() else list.head
        override fun toString(): String = list.toString()
    }
}

interface IPStack<T> {
    val isEmpty: Boolean
    val isNotEmpty: Boolean get() = !isEmpty

    fun push(element: T): IPStack<T>
    fun pop(): IPStack<T>
    fun peek(): T
}

abstract class PersistentStack<T> : IPStack<T> {
    companion object {
        fun <T> emptyStack(): IPStack<T> = object : PersistentStack<T>() {
            override val isEmpty: Boolean get() = true
            override fun pop(): IPStack<T> = throw NoSuchElementException()
            override fun peek(): T = throw NoSuchElementException()
            override fun toString(): String = "Empty"
        }
    }

    abstract override fun pop(): IPStack<T>
    abstract override fun peek(): T

    abstract override val isEmpty: Boolean

    override fun push(element: T): PersistentStack<T> = LinkNode(this, element)

    inner class LinkNode(private val previous: IPStack<T>, private val element: T) : PersistentStack<T>() {
        override fun pop(): IPStack<T> = previous
        override val isEmpty: Boolean get() = false

        override fun peek(): T = element
        override fun toString(): String = "$element"
    }
}

fun main() {
    println("stack")
    var s = PersistentStack.emptyStack<Int>()
            .push(1)
            .push(2)
            .push(3)
    while (s.isNotEmpty) {
        println(s.peek())
        s = s.pop()
    }

    println("queue")
    var q = PersistentQueue.emptyQueue<Int>()
            .put(1)
            .put(2)
            .put(3)
    while (q.isNotEmpty) {
        println(q.peek())
        q = q.dequeue()
    }
}
