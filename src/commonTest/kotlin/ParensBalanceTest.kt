import kotlin.test.Test

class ParensBalanceTest {
    private val roundBrackets = '(' to ')'
    private val squareBrackets = '[' to ']'
    private val curlyBrackets = '{' to '}'
    private val angleBrackets = '<' to '>'

    @Test
    fun `round brackets`() {
        "()" shouldHaveBalanced roundBrackets
        "((()))" shouldHaveBalanced roundBrackets
        "()()" shouldHaveBalanced roundBrackets
        "())" shouldNotHaveBalanced roundBrackets
        ")(" shouldNotHaveBalanced roundBrackets
        "())(" shouldNotHaveBalanced roundBrackets
    }

    @Test
    fun `square brackets`() {
        "[]" shouldHaveBalanced squareBrackets
        "[[[]]]" shouldHaveBalanced squareBrackets
        "[][]" shouldHaveBalanced squareBrackets
        "[]]" shouldNotHaveBalanced squareBrackets
        "][" shouldNotHaveBalanced squareBrackets
        "[]][" shouldNotHaveBalanced squareBrackets
    }

    @Test
    fun `curly brackets`() {
        "{}" shouldHaveBalanced curlyBrackets
        "{{{}}}" shouldHaveBalanced curlyBrackets
        "{}{}" shouldHaveBalanced curlyBrackets
        "{}}" shouldNotHaveBalanced curlyBrackets
        "}{" shouldNotHaveBalanced curlyBrackets
        "{}}{" shouldNotHaveBalanced curlyBrackets
    }

    @Test
    fun `angle brackets`() {
        "<>" shouldHaveBalanced angleBrackets
        "<<<>>>" shouldHaveBalanced angleBrackets
        "<><>" shouldHaveBalanced angleBrackets
        "<>>" shouldNotHaveBalanced angleBrackets
        "><" shouldNotHaveBalanced angleBrackets
        "<>><" shouldNotHaveBalanced angleBrackets
    }
}