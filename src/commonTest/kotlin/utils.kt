import klisp.eval
import klisp.exp
import klisp.parse

@ExperimentalUnsignedTypes
fun _eval(s: String): exp = eval(parse(s))
        .also { println("$s -> $it") }