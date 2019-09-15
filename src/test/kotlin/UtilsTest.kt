import klisp.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilsTest {
    @Test
    fun `string utils`() {
        assertEquals('a', "ab".head)
        assertEquals('a', "ab".first)
        assertEquals("b", "ab".tail)
        assertEquals("b", "ab".rest)
        assertEquals('b', "ab".last)

        assertEquals('a', "a".head)
        assertEquals('a', "a".first)
        assertEquals("", "a".tail)
        assertEquals("", "a".rest)
        assertEquals('a', "a".last)

        assertEquals('a', "abc".head)
        assertEquals('a', "abc".first)
        assertEquals("bc", "abc".tail)
        assertEquals("bc", "abc".rest)
        assertEquals('c', "abc".last)

        assertFailsWith(NoSuchElementException::class) { "".head }
        assertFailsWith(NoSuchElementException::class) { "".first }
        assertFailsWith(NoSuchElementException::class) { "".rest }
        assertFailsWith(NoSuchElementException::class) { "".tail }
        assertFailsWith(NoSuchElementException::class) { "".last }
    }

    @Test
    fun `list utils`() {
        assertEquals('a', "ab".toList().head)
        assertEquals('a', "ab".toList().first)
        assertEquals('b', "ab".toList().last)
        assertEquals("b".toList(), "ab".toList().tail)
        assertEquals("b".toList(), "ab".toList().rest)

        assertEquals('a', "a".toList().head)
        assertEquals('a', "a".toList().first)
        assertEquals('a', "a".toList().last)
        assertEquals("".toList(), "a".toList().tail)
        assertEquals("".toList(), "a".toList().rest)

        assertEquals('a', "abc".toList().head)
        assertEquals('a', "abc".toList().first)
        assertEquals('c', "abc".toList().last)
        assertEquals("bc".toList(), "abc".toList().tail)
        assertEquals("bc".toList(), "abc".toList().rest)

        assertFailsWith(NoSuchElementException::class) { "".toList().head }
        assertFailsWith(NoSuchElementException::class) { "".toList().first }
        assertFailsWith(NoSuchElementException::class) { "".toList().rest }
        assertFailsWith(NoSuchElementException::class) { "".toList().tail }
        assertFailsWith(NoSuchElementException::class) { "".toList().last }
    }

    @Test
    fun destructuring() {
        assertEquals(
            (listOf("a", listOf("b"))).toString(),
            ("ab".toList() unApply 2).toString()
        )

        assertEquals(
            (listOf("a", listOf("b", "c"))).toString(),
            ("abc".toList() unApply 2).toString()
        )

        assertEquals(
            (listOf("a", "b", listOf("c"))).toString(),
            ("abc".toList() unApply 3).toString()
        )

        assertEquals(
            (listOf((1), (2), (3), listOf((4)))).toString(),
            (listOf(1, 2, 3, 4) unApply 4).toString()
        )

        assertEquals(
            (listOf((1), null, (3), listOf((4)))).toString(),
            (listOf(1, null, 3, 4) unApply 4).toString()
        )

        assertEquals(
            (listOf((1), (2), emptyList<Any>())).toString(),
            (listOf(1, 2) unApply 3).toString()
        )

        assertEquals(
            (listOf((1), (2), null, emptyList<Any>())).toString(),
            (listOf(1, 2) unApply 4).toString()
        )

        assertFailsWith(IllegalArgumentException::class) { listOf(1) unApply 1 }
    }

    @Test
    fun `plist without exclusions`() {
        val a = arrayOf(
            "a".toKeyword(),
            "b".toKeyword(),
            "c".toKeyword(),
            "d".toKeyword(),
            "e".toKeyword(),
            "f".toKeyword()
        ).toPlist()

        val e = mapOf(
            "a".toKeyword() to "b".toKeyword(),
            "c".toKeyword() to "d".toKeyword(),
            "e".toKeyword() to "f".toKeyword()
        )

        assertEquals(e, a)
    }

    @Test
    fun `plist with exclusions`() {
        val a = arrayOf(
            "a".toKeyword(),
            string("b"),
            "c".toKeyword(),
            string("d"),
            "e".toKeyword(),
            "f".toKeyword()
        ).toPlist(listOf("e", "f"))

        val e = mapOf(
            "a".toKeyword() to string("b"),
            "c".toKeyword() to string("d"),
            "e".toKeyword() to "e".toKeyword(),
            "f".toKeyword() to "f".toKeyword()
        )

        assertEquals(e, a)
    }
}