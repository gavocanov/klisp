import klisp.bool
import klisp.eval
import klisp.exp
import klisp.parse
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@ImplicitReflectionSerializer
@ExperimentalUnsignedTypes
fun _eval(s: String): exp = eval(parse(s))
        .also { println("$s -> $it") }

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
private fun _eq(s: String, exp: exp) = assertEquals(exp, _eval(s), s)

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.eq(exp: exp) = _eq(this, exp)

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.shouldEvalTo(exp: exp) = _eq(this, exp)

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.shouldEvalTo(b: Boolean) = _eq(this, bool(b))

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.shouldBe(b: Boolean) = _eq(this, bool(b) as exp)

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.shouldNotBe(b: Boolean) = assertNotEquals(b, (_eval(this) as bool).value, this)

@ExperimentalUnsignedTypes
@ImplicitReflectionSerializer
infix fun String.shouldBeA(clazz: KClass<*>) = assertEquals(clazz, _eval(this)::class, this)

infix fun String.shouldEqual(s: String) = assertEquals(this, s, "$this equals $s")
