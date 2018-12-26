import klisp.byte
import klisp.char
import klisp.float
import klisp.func
import klisp.keyword
import klisp.list
import klisp.long
import klisp.map
import klisp.set
import klisp.string
import klisp.symbol
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.test.Test

@ImplicitReflectionSerializer
@ExperimentalUnsignedTypes
class SpecialFormsTest {
    @Test
    fun `fmap tests`() {
        """
        (fmap (lam (x) (+ 1 x)) (range 1 4))
        """.trimIndent() shouldEvalTo list(listOf(long(2), long(3), long(4), long(5)))
    }

    @Test
    fun `filter tests`() {
        """
        (filter (lam (x) (> x 1)) (range 1 4))
        """.trimIndent() shouldEvalTo list(listOf(long(2), long(3), long(4)))
    }

    @Test
    fun `reduce tests`() {
        """
        (reduce 0 + (range 1 4))
        """.trimIndent() shouldEvalTo long(10)

        """
        (reduce 0
                (lam (a n)
                     (if (> a n)
                         a
                         n))
                (range 1 4))
        """.trimIndent() shouldEvalTo long(4)
    }

    @Test
    fun `def tests`() {
        "(def a 1)" shouldEvalTo byte(1)
        "(def a 1.0)" shouldEvalTo float(1.0f)
        "(def a true)" shouldEvalTo true
        "(def a false)" shouldEvalTo false
        "(def a '1')" shouldEvalTo char('1')
        "(def a \"11 aa\")" shouldEvalTo string("\"11 aa\"")
        "(def a (lam (x) (+ x 1)))" shouldBeA func::class
        "(def a (list 1))" shouldBeA list::class
        "(def a (set 1))" shouldBeA set::class
        "(def a (map :a 1))" shouldBeA map::class
        "(def a :a)" shouldEvalTo keyword(":a")
    }

    @Test
    fun `lambda's`() {
        "(lam (a) (a))" shouldBeA func::class
        "(def id (lam (a) (a)))" shouldBeA func::class
        // TODO need identity or eval
        "(fmap (lam (a) (def b a)) (list 1 2))" shouldEvalTo list(listOf(byte(1), byte(2)))
    }

    @Test
    fun `quote stuff`() {
        // TODO should check...
        "(quote (be ba))" shouldEvalTo list(listOf(symbol("be"), symbol("ba")))
        "(quote (+ ba))" shouldEvalTo list(listOf(symbol("+"), symbol("ba")))
    }
}