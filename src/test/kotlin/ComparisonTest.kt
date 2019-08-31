import kotlin.test.Test

@ExperimentalUnsignedTypes
class ComparisonTest {
    @Test
    fun gt() {
        "(> 2 1)" shouldEvalTo true
        "(> .2 .1)" shouldEvalTo true
        "(> 2 3)" shouldEvalTo false
        "(> .2 .3)" shouldEvalTo false
    }

    @Test
    fun gte() {
        "(>= 2 2)" shouldEvalTo true
        "(>= .2 .2)" shouldEvalTo true
        "(>= 2 4)" shouldEvalTo false
        "(>= .2 .4)" shouldEvalTo false
    }

    @Test
    fun lt() {
        "(< 1 2)" shouldEvalTo true
        "(< .1 .2)" shouldEvalTo true
        "(< 2 2)" shouldEvalTo false
        "(< .2 .2)" shouldEvalTo false
    }

    @Test
    fun lte() {
        "(<= 2 2)" shouldEvalTo true
        "(<= .2 .2)" shouldEvalTo true
        "(<= 5 2)" shouldEvalTo false
        "(<= .5 .2)" shouldEvalTo false
    }

    @Test
    fun `equality =`() {
        "(= 2 2)" shouldEvalTo true
        "(= .2 .2)" shouldEvalTo true
        "(= 2 3)" shouldEvalTo false
        "(= .2 .3)" shouldEvalTo false
        "(= true false)" shouldEvalTo false
        "(= true true)" shouldEvalTo true
        "(= false false)" shouldEvalTo true
    }

    @Test
    fun `equality eq?`() {
        "(eq? 2 2)" shouldEvalTo true
        "(eq? .2 .2)" shouldEvalTo true
        "(eq? 2 1)" shouldEvalTo false
        "(eq? .2 .1)" shouldEvalTo false
        "(eq? true false)" shouldEvalTo false
        "(eq? true true)" shouldEvalTo true
        "(eq? false false)" shouldEvalTo true
    }

    @Test
    fun str() {
        "(string? \"1\")" shouldEvalTo true
        "(string? 1)" shouldEvalTo false
    }

    @Test
    fun chr() {
        "(char? \\1)" shouldEvalTo true
        "(char? 1)" shouldEvalTo false
        "(char? \"1\")" shouldEvalTo false
    }

    @Test
    fun num() {
        "(number? 1)" shouldEvalTo true
        "(number? 1.011)" shouldEvalTo true
        "(number? true)" shouldEvalTo true
        "(number? false)" shouldEvalTo true
        "(number? (list))" shouldEvalTo false
    }

    @Test
    fun integer() {
        "(integer? 1)" shouldEvalTo true
        "(integer? 1.011)" shouldEvalTo false
        "(integer? true)" shouldEvalTo true
        "(integer? false)" shouldEvalTo true
        "(integer? (list))" shouldEvalTo false
    }

    @Test
    fun dec() {
        "(decimal? 1)" shouldEvalTo false
        "(decimal? 1.011)" shouldEvalTo true
        "(decimal? true)" shouldEvalTo false
        "(decimal? false)" shouldEvalTo false
        "(decimal? (list))" shouldEvalTo false
    }

    @Test
    fun keyword() {
        "(keyword? :a)" shouldEvalTo true
        "(keyword? 1)" shouldEvalTo false
    }

    @Test
    fun atom() {
        "(atom? pi)" shouldEvalTo true
        "(atom? 1)" shouldEvalTo true
        "(atom? true)" shouldEvalTo true
        "(atom? false)" shouldEvalTo true
        "(atom? .1)" shouldEvalTo true
        "(atom? \\1)" shouldEvalTo true
        "(atom? \"1\")" shouldEvalTo true
        "(atom? (def a 1))" shouldEvalTo true
        "(atom? (map :a 1))" shouldEvalTo true
        "(atom? (list :a 1))" shouldEvalTo true
        "(atom? (set :a 1))" shouldEvalTo true

        "(atom? +)" shouldEvalTo false
        "(atom? (lam (x) (+ 1 x)))" shouldEvalTo false
    }

    @Test
    fun list() {
        "(list? (list 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo true
        "(list? (set 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo false
        "(list? (map :a 1))" shouldEvalTo false
    }

    @Test
    fun set() {
        "(set? (list 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo false
        "(set? (set 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo true
        "(set? (map :a 1))" shouldEvalTo false
    }

    @Test
    fun collection() {
        "(collection? (list 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo true
        "(collection? (set 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo true
        "(collection? (map :a 1))" shouldEvalTo false
    }

    @Test
    fun map() {
        "(map? (list 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo false
        "(map? (set 1 2 3 (list 1 (set 1 2 1 2))))" shouldEvalTo false
        "(map? (map :a 1))" shouldEvalTo true
    }

    @Test
    fun isa() {
        "(is? pi pi)" shouldEvalTo true
        "(is? pi PI)" shouldEvalTo false
    }
}