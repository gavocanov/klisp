import klisp.byte
import klisp.double
import klisp.long
import klisp.unit
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.test.Test

@ExperimentalUnsignedTypes

class ConditionTest {
    @Test
    fun `if - then | else`() {
        """
        (if (> 2 1) true false)
        """.trimIndent() shouldEvalTo true

        """
        (if (> 1 1) (+ 1 1) false)
        """.trimIndent() shouldEvalTo false

        """
        (if (> 1 1) (+ 1 1) (abs -100))
        """.trimIndent() shouldEvalTo long(100)

        """
        (if (> 2 1) (+ 1 1) false)
        """.trimIndent() shouldEvalTo long(2)
    }

    @Test
    fun `when - then`() {
        """
            (when true 1)
        """.trimIndent() shouldEvalTo byte(1)

        """
            (when true (pow 2 2))
        """.trimIndent() shouldEvalTo double(4.0)

        """
            (when (> 1 2) (pow 2 2))
        """.trimIndent() shouldEvalTo unit
    }

    @Test
    fun `unless - then`() {
        """
            (unless true 1)
        """.trimIndent() shouldEvalTo unit

        """
            (unless true (pow 2 2))
        """.trimIndent() shouldEvalTo unit

        """
            (unless (> 1 2) (pow 2 2))
        """.trimIndent() shouldEvalTo double(4.0)
    }
}