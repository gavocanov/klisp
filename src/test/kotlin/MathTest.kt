import klisp.*
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalUnsignedTypes

class MathTest {
    @Test
    fun `plus bytes`() {
        assertTrue { _eval("1") is byte }
        assertTrue { _eval("2") is byte }
        val a = _eval("""
            (+ 1 2)
        """.trimIndent())
        assertEquals(long(1 + 2), a)
    }

    @Test
    fun `minus short`() {
        assertTrue { _eval("257") is short }
        val a = _eval("""
            (- 257 257)
        """.trimIndent())
        assertEquals(long(257 - 257), a)
    }

    @Test
    fun `mul short`() {
        assertTrue { _eval("257") is short }
        val a = _eval("""
            (* 257 257)
        """.trimIndent())
        assertEquals(ulong.from(257 * 257), a)
    }

    @Test
    fun `div short`() {
        assertTrue { _eval("257") is short }
        val a = _eval("""
            (/ 257 2)
        """.trimIndent())
        assertEquals(double.from(257 / 2.0), a)
    }

    @Test
    fun `pow int`() {
        assertTrue { _eval("70000") is int }
        val a = _eval("""
            (^ 70000 2)
        """.trimIndent())
        assertEquals(double(70000 * 70000.0), a)
    }

    @Test
    fun `mod int`() {
        assertTrue { _eval("70000") is int }
        val a = _eval("""
            (% 70000 3)
        """.trimIndent())
        assertEquals(double.from(70000 % 3), a)
    }

    @Test
    fun `abs long`() {
        assertTrue { _eval("123456789123") is long }
        val a = _eval("""
            (abs -123456789123)
        """.trimIndent())
        assertEquals(long(123456789123), a)
    }

    @Test
    fun `plus unsigned long`() {
        assertTrue { _eval((ULong.MAX_VALUE / 2u).toString()) is long }
        val o = ULong.MAX_VALUE / 2u
        val a = _eval("""
            (+ $o $o)
        """.trimIndent())
        assertEquals(ulong(o.plus(o)), a)
    }

    @Test
    fun `mul unsigned long`() {
        assertTrue { _eval((ULong.MAX_VALUE - 10u).toString()) is ulong }
        val o = ULong.MAX_VALUE - 10u
        val a = _eval("""
            (* $o 2)
        """.trimIndent())
        // nat failing without .toString
        assertEquals(double.from(o.toString().toDouble() * 2).toString(), a.toString())
    }

    @Test
    fun `div float`() {
        assertTrue { _eval("1.0") is float }
        assertTrue { _eval("0.11") is float }
        val a = _eval("""
            (/ 1.0 0.11)
        """.trimIndent())
        assertEquals(double(1.0 / 0.11f), a)
    }

    @Test
    fun `mul float`() {
        assertTrue { _eval("1.0") is float }
        assertTrue { _eval("0.11") is float }
        val a = _eval("""
            (* 1.0 0.11)
        """.trimIndent())
        assertEquals(double(1.0 * 0.11f), a)
    }

    @Test
    fun `mul double`() {
        val o = -0.616e128
        assertTrue { _eval(o.toString()) is double }
        val a = _eval("""
            (* $o $o)
        """.trimIndent())
        assertEquals(double(o * o), a)
    }

    @Test
    fun `div double`() {
        val o = -0.616e128
        assertTrue { _eval(o.toString()) is double }
        val a = _eval("""
            (/ $o 2)
        """.trimIndent())
        assertEquals(double(o / 2), a)
    }
}
