import klisp.bool
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class ComparisonTest {
    @Test
    fun gt() {
        assertEquals(bool(true), _eval("(> 2 1)"))
        assertEquals(bool(true), _eval("(> .2 .1)"))
        assertEquals(bool(false), _eval("(> 2 3)"))
        assertEquals(bool(false), _eval("(> .2 .3)"))
    }

    @Test
    fun gte() {
        assertEquals(bool(true), _eval("(>= 2 2)"))
        assertEquals(bool(true), _eval("(>= .2 .2)"))
        assertEquals(bool(false), _eval("(>= 2 4)"))
        assertEquals(bool(false), _eval("(>= .2 .4)"))
    }

    @Test
    fun lt() {
        assertEquals(bool(true), _eval("(< 1 2)"))
        assertEquals(bool(true), _eval("(< .1 .2)"))
        assertEquals(bool(false), _eval("(< 2 2)"))
        assertEquals(bool(false), _eval("(< .2 .2)"))
    }

    @Test
    fun lte() {
        assertEquals(bool(true), _eval("(<= 2 2)"))
        assertEquals(bool(true), _eval("(<= .2 .2)"))
        assertEquals(bool(false), _eval("(<= 5 2)"))
        assertEquals(bool(false), _eval("(<= .5 .2)"))
    }

    @Test
    fun `equality =`() {
        assertEquals(bool(true), _eval("(= 2 2)"))
        assertEquals(bool(true), _eval("(= .2 .2)"))
        assertEquals(bool(false), _eval("(= 2 3)"))
        assertEquals(bool(false), _eval("(= .2 .3)"))
        assertEquals(bool(false), _eval("(= true false)"))
        assertEquals(bool(true), _eval("(= true true)"))
        assertEquals(bool(true), _eval("(= false false)"))
    }

    @Test
    fun `equality eq?`() {
        assertEquals(bool(true), _eval("(eq? 2 2)"))
        assertEquals(bool(true), _eval("(eq? .2 .2)"))
        assertEquals(bool(false), _eval("(eq? 2 1)"))
        assertEquals(bool(false), _eval("(eq? .2 .1)"))
        assertEquals(bool(false), _eval("(eq? true false)"))
        assertEquals(bool(true), _eval("(eq? true true)"))
        assertEquals(bool(true), _eval("(eq? false false)"))
    }

    @Test
    fun str() {
        assertEquals(bool(true), _eval("(string? \"1\")"))
        assertEquals(bool(false), _eval("(string? 1)"))
    }

    @Test
    fun chr() {
        assertEquals(bool(true), _eval("(char? '1')"))
        assertEquals(bool(false), _eval("(char? 1)"))
        assertEquals(bool(false), _eval("(char? \"1\")"))
    }

    @Test
    fun num() {
        assertEquals(bool(true), _eval("(number? 1)"))
        assertEquals(bool(true), _eval("(number? 1.011)"))
        assertEquals(bool(true), _eval("(number? true)"))
        assertEquals(bool(true), _eval("(number? false)"))
        assertEquals(bool(false), _eval("(number? (list))"), "list")
    }

    @Test
    fun integer() {
        assertEquals(bool(true), _eval("(integer? 1)"))
        assertEquals(bool(false), _eval("(integer? 1.011)"))
        assertEquals(bool(true), _eval("(integer? true)"))
        assertEquals(bool(true), _eval("(integer? false)"))
        assertEquals(bool(false), _eval("(integer? (list))"), "list")
    }

    @Test
    fun dec() {
        assertEquals(bool(false), _eval("(decimal? 1)"))
        assertEquals(bool(true), _eval("(decimal? 1.011)"))
        assertEquals(bool(false), _eval("(decimal? true)"))
        assertEquals(bool(false), _eval("(decimal? false)"))
        assertEquals(bool(false), _eval("(decimal? (list))"), "list")
    }

    @Test
    fun keyword() {
        assertEquals(bool(true), _eval("(keyword? :a)"))
        assertEquals(bool(false), _eval("(keyword? 1)"))
    }

    @Test
    fun atom() {
        assertEquals(bool(true), _eval("(atom? pi)"))
        assertEquals(bool(true), _eval("(atom? 1)"))
        assertEquals(bool(true), _eval("(atom? true)"))
        assertEquals(bool(true), _eval("(atom? false)"))
        assertEquals(bool(true), _eval("(atom? .1)"))
        assertEquals(bool(true), _eval("(atom? '1')"))
        assertEquals(bool(true), _eval("(atom? \"1\")"))
        assertEquals(bool(true), _eval("(atom? (def a 1))"))
        assertEquals(bool(true), _eval("(atom? (map :a 1))"))
        assertEquals(bool(true), _eval("(atom? (list :a 1))"))
        assertEquals(bool(true), _eval("(atom? (set :a 1))"))

        assertEquals(bool(false), _eval("(atom? +)"))
        assertEquals(bool(false), _eval("(atom? (lam (x) (+ 1 x)))"))
    }

    @Test
    fun list() {
        assertEquals(bool(true), _eval("(list? (list 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(false), _eval("(list? (set 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(false), _eval("(list? (map :a 1))"))
    }

    @Test
    fun set() {
        assertEquals(bool(false), _eval("(set? (list 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(true), _eval("(set? (set 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(false), _eval("(set? (map :a 1))"))
    }

    @Test
    fun collection() {
        assertEquals(bool(true), _eval("(collection? (list 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(true), _eval("(collection? (set 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(false), _eval("(collection? (map :a 1))"))
    }

    @Test
    fun map() {
        assertEquals(bool(false), _eval("(map? (list 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(false), _eval("(map? (set 1 2 3 (list 1 (set 1 2 1 2))))"))
        assertEquals(bool(true), _eval("(map? (map :a 1))"))
    }
}