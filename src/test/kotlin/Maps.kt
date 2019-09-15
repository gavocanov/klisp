import klisp.*
import kotlin.test.Test

class Maps {
    @Test
    fun `map get`() {
        "(:id (map :id 1))" shouldEvalTo byte(1)
        "(:id (map :id true))" shouldEvalTo bool(true)
        "(:id (map :id (list 1 2)))" shouldEvalTo list(listOf(byte(1), byte(2)))
    }
}
