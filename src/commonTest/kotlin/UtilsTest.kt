import klisp.None
import klisp.Some
import klisp.first
import klisp.head
import klisp.last
import klisp.rest
import klisp.tail
import klisp.unApply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilsTest {
    @Test
    fun `string utils`() {
        assertEquals("ab".head, 'a')
        assertEquals("ab".first, 'a')
        assertEquals("ab".tail, "b")
        assertEquals("ab".rest, "b")
        assertEquals("ab".last, 'b')

        assertEquals("a".head, 'a')
        assertEquals("a".first, 'a')
        assertEquals("a".tail, "")
        assertEquals("a".rest, "")
        assertEquals("a".last, 'a')

        assertEquals("abc".head, 'a')
        assertEquals("abc".first, 'a')
        assertEquals("abc".tail, "bc")
        assertEquals("abc".rest, "bc")
        assertEquals("abc".last, 'c')

        assertFailsWith(NoSuchElementException::class) { "".head }
        assertFailsWith(NoSuchElementException::class) { "".first }
        assertFailsWith(NoSuchElementException::class) { "".rest }
        assertFailsWith(NoSuchElementException::class) { "".tail }
        assertFailsWith(NoSuchElementException::class) { "".last }
    }

    @Test
    fun `list utils`() {
        assertEquals("ab".toList().head, 'a')
        assertEquals("ab".toList().first, 'a')
        assertEquals("ab".toList().last, 'b')
        assertEquals("ab".toList().tail, "b".toList())
        assertEquals("ab".toList().rest, "b".toList())

        assertEquals("a".toList().head, 'a')
        assertEquals("a".toList().first, 'a')
        assertEquals("a".toList().last, 'a')
        assertEquals("a".toList().tail, "".toList())
        assertEquals("a".toList().rest, "".toList())

        assertEquals("abc".toList().head, 'a')
        assertEquals("abc".toList().first, 'a')
        assertEquals("abc".toList().last, 'c')
        assertEquals("abc".toList().tail, "bc".toList())
        assertEquals("abc".toList().rest, "bc".toList())

        assertFailsWith(NoSuchElementException::class) { "".toList().head }
        assertFailsWith(NoSuchElementException::class) { "".toList().first }
        assertFailsWith(NoSuchElementException::class) { "".toList().rest }
        assertFailsWith(NoSuchElementException::class) { "".toList().tail }
        assertFailsWith(NoSuchElementException::class) { "".toList().last }
    }

    @Test
    fun destructuring() {
        assertEquals(
                (listOf(Some("a"), listOf(Some("b")))).toString(),
                ("ab".toList() unApply 2).toString()
        )

        assertEquals(
                (listOf(Some("a"), listOf(Some("b"), Some("c")))).toString(),
                ("abc".toList() unApply 2).toString()
        )

        assertEquals(
                (listOf(Some("a"), Some("b"), listOf(Some("c")))).toString(),
                ("abc".toList() unApply 3).toString()
        )

        assertEquals(
                (listOf(Some(1), Some(2), Some(3), listOf(Some(4)))).toString(),
                (listOf(1, 2, 3, 4) unApply 4).toString()
        )

        assertEquals(
                (listOf(Some(1), None, Some(3), listOf(Some(4)))).toString(),
                (listOf(1, null, 3, 4) unApply 4).toString()
        )

        assertEquals(
                (listOf(Some(1), Some(2), emptyList<Any>())).toString(),
                (listOf(1, 2) unApply 3).toString()
        )

        assertEquals(
                (listOf(Some(1), Some(2), None, emptyList<Any>())).toString(),
                (listOf(1, 2) unApply 4).toString()
        )

        assertFailsWith(IllegalArgumentException::class) { listOf(1) unApply 1 }
    }
}