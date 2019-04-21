import klisp.byte
import klisp.list
import klisp.long
import kotlin.test.Test

@ExperimentalUnsignedTypes
class MiscFuncsTest {
    @Test
    fun `range test`() {
        "(range 1 3)" shouldEvalTo list(listOf(long(1), long(2), long(3)))
    }

    @Test
    fun `misc collections stuff`() {
        "(head (list 1 2 3))" shouldEvalTo byte(1)
        "(first (list 1 2 3))" shouldEvalTo byte(1)
        "(car (list 1 2 3))" shouldEvalTo byte(1)

        "(tail (list 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))
        "(rest (list 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))
        "(cdr (list 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))

        "(last (list 1 2 3))" shouldEvalTo byte(3)

        "(head (set 1 2 3))" shouldEvalTo byte(1)
        "(first (set 1 2 3))" shouldEvalTo byte(1)
        "(car (set 1 2 3))" shouldEvalTo byte(1)

        "(tail (set 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))
        "(rest (set 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))
        "(cdr (set 1 2 3))" shouldEvalTo list(listOf(byte(2), byte(3)))

        "(last (list 1 2 3))" shouldEvalTo byte(3)
    }
}