@file:Suppress("EXPERIMENTAL_API_USAGE", "TestFunctionName", "unused")

import klisp.bool
import klisp.eval
import klisp.exp
import klisp.ok
import klisp.parser.derivativeParse
import klisp.parser.hasBalancedPairOf
import klisp.parser.lexer.KLispLexer
import klisp.parser.lexer.LiveStream
import klisp.parser.lexer.tokens.DecToken
import klisp.parser.lexer.tokens.IntToken
import klisp.parser.lexer.tokens.Token
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

fun _eval(s: String): exp = eval(derivativeParse(s), false)

private fun _eq(s: String, exp: exp) = assertEquals(exp, _eval(s), s)

infix fun String.eq(exp: exp) = _eq(this, exp)
infix fun String.shouldEvalTo(exp: exp) = _eq(this, exp)
infix fun String.shouldEvalTo(b: Boolean) = _eq(this, bool(b))
infix fun String.shouldBe(b: Boolean) = _eq(this, bool(b) as exp)
infix fun String.shouldNotBe(b: Boolean) = assertNotEquals(b, (_eval(this) as bool).value, this)
infix fun String.shouldBeA(clazz: KClass<*>) = assertEquals(clazz, _eval(this)::class, this)

private fun lex(s: String): List<Token> {
    val l = KLispLexer()
    l.lex(LiveStream(s))
    return l.output.toList
}

private fun lexSingle(s: String): Token {
    val l = KLispLexer()
    l.lex(LiveStream(s))
    assertEquals(1, l.output.toList.size, "lexing output size")
    return l.output.head
}

infix fun String.shouldLexTo(token: Token) {
    val actual = lexSingle(this)
    assertEquals(token.toString(), actual.toString())
    assertEquals(token::class, actual::class)
    when (token) {
        is DecToken -> assertEquals(this.toDouble(), token.value.toDouble(), "actual typed value not equal, $this - $token")
        is IntToken -> assertEquals(this.toLong(), token.value.toLong(), "actual typed value not equal, $this - $token")
    }
}

infix fun <T : Token> String.shouldFailToLexAs(token: KClass<T>) {
    val actual = lexSingle(this)
    assertNotEquals(token, actual::class)
}

infix fun <T : Throwable> String.shouldFailToLexWith(exception: KClass<T>): T =
    assertFailsWith(exception) { lexSingle(this) }

infix fun String.shouldEqual(s: String) = assertEquals(this, s, "$this equals $s")
infix fun String.shouldHaveBalanced(p: Pair<Char, Char>) = assertEquals(true, this.hasBalancedPairOf(p).ok, "$this has balanced $p")
infix fun String.shouldNotHaveBalanced(p: Pair<Char, Char>) = assertNotEquals(true, this.hasBalancedPairOf(p).ok, "$this does not have balanced $p")
