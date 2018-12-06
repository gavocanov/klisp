package klisp

import kotlin.math.PI

val stdEnv: MutableMap<symbol, exp> = mutableMapOf(
        symbol("MAX_BYTE") to byte(Byte.MAX_VALUE),
        symbol("MAX_SHORT") to short(Short.MAX_VALUE),
        symbol("MAX_INT") to int(Int.MAX_VALUE),
        symbol("MAX_LONG") to long(Long.MAX_VALUE),
        symbol("MAX_FLOAT") to float(Float.MAX_VALUE),
        symbol("MAX_DOUBLE") to double(Double.MAX_VALUE),
        symbol("PI") to double(PI),
        symbol("pi") to double(PI),
        symbol("true") to bool(true),
        symbol("false") to bool(false),
        symbol("begin") to func(::begin),
        symbol("range") to func(::range),
        symbol("list") to func(::list),
        symbol("+") to func { foldableMathOp(mathOp.plus, it) },
        symbol("-") to func { foldableMathOp(mathOp.minus, it) },
        symbol("*") to func { foldableMathOp(mathOp.mul, it) },
        symbol("/") to func { foldableMathOp(mathOp.div, it) },
        symbol(">") to func { it: List<exp> -> bool(compare(compareOp.gt, it)) },
        symbol(">=") to func { it: List<exp> -> bool(compare(compareOp.gte, it)) },
        symbol("=>") to func { it: List<exp> -> bool(compare(compareOp.gte, it)) },
        symbol("<") to func { it: List<exp> -> bool(compare(compareOp.lt, it)) },
        symbol("<=") to func { it: List<exp> -> bool(compare(compareOp.lte, it)) },
        symbol("=<") to func { it: List<exp> -> bool(compare(compareOp.lte, it)) },
        symbol("double?") to func { it: List<exp> -> isa(type.double, it) },
        symbol("float?") to func { it: List<exp> -> isa(type.float, it) },
        symbol("long?") to func { it: List<exp> -> isa(type.long, it) },
        symbol("int?") to func { it: List<exp> -> isa(type.int, it) },
        symbol("short?") to func { it: List<exp> -> isa(type.short, it) },
        symbol("byte?") to func { it: List<exp> -> isa(type.byte, it) },
        symbol("char?") to func { it: List<exp> -> isa(type.char, it) },
        symbol("symbol?") to func { it: List<exp> -> isa(type.string, it) },
        symbol("is?") to func(::_is),
        symbol("eq?") to func(::eq)
)

