import klisp.parser.PP
import klisp.bool
import klisp.eval
import klisp.exp
import klisp.parser.hasBalancedPairOf
import klisp.ok
import klisp.parser.derivativeParse
import klisp.parser.regexParse
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private const val DEBUG = false

@ExperimentalUnsignedTypes
fun _eval(s: String): exp = eval(derivativeParse(s))
        .also { if (DEBUG) println("$s -> $it") }

@ExperimentalUnsignedTypes
private fun _eq(s: String, exp: exp) = assertEquals(exp, _eval(s), s)

@ExperimentalUnsignedTypes
infix fun String.eq(exp: exp) = _eq(this, exp)

@ExperimentalUnsignedTypes
infix fun String.shouldEvalTo(exp: exp) = _eq(this, exp)

@ExperimentalUnsignedTypes
infix fun String.shouldEvalTo(b: Boolean) = _eq(this, bool(b))

@ExperimentalUnsignedTypes
infix fun String.shouldBe(b: Boolean) = _eq(this, bool(b) as exp)

@ExperimentalUnsignedTypes
infix fun String.shouldNotBe(b: Boolean) = assertNotEquals(b, (_eval(this) as bool).value, this)

@ExperimentalUnsignedTypes
infix fun String.shouldBeA(clazz: KClass<*>) = assertEquals(clazz, _eval(this)::class, this)

infix fun String.shouldEqual(s: String) = assertEquals(this, s, "$this equals $s")
infix fun String.shouldHaveBalanced(p: PP) = assertEquals(true, this.hasBalancedPairOf(p).ok, "$this has balanced $p")
infix fun String.shouldNotHaveBalanced(p: PP) = assertNotEquals(true, this.hasBalancedPairOf(p).ok, "$this does not have balanced $p")
