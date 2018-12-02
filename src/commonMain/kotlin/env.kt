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
        symbol("true") to bool(true),
        symbol("false") to bool(false),
        symbol("begin") to func(::begin),
        symbol("*") to func(::mul),
        symbol("+") to func(::plus),
        symbol("-") to func(::minus),
        symbol("/") to func(::div),
        symbol(">") to func { it: List<exp> -> bool(compare(CompareOps.gt, it)) },
        symbol(">=") to func { it: List<exp> -> bool(compare(CompareOps.gte, it)) },
        symbol("=>") to func { it: List<exp> -> bool(compare(CompareOps.gte, it)) },
        symbol("<") to func { it: List<exp> -> bool(compare(CompareOps.lt, it)) },
        symbol("<=") to func { it: List<exp> -> bool(compare(CompareOps.lte, it)) },
        symbol("=<") to func { it: List<exp> -> bool(compare(CompareOps.lte, it)) },
        symbol("double?") to func { it: List<exp> -> isa(Types.double, it) },
        symbol("float?") to func { it: List<exp> -> isa(Types.float, it) },
        symbol("long?") to func { it: List<exp> -> isa(Types.long, it) },
        symbol("int?") to func { it: List<exp> -> isa(Types.int, it) },
        symbol("short?") to func { it: List<exp> -> isa(Types.short, it) },
        symbol("byte?") to func { it: List<exp> -> isa(Types.byte, it) },
        symbol("char?") to func { it: List<exp> -> isa(Types.char, it) },
        symbol("symbol?") to func { it: List<exp> -> isa(Types.string, it) },
        symbol("is?") to func(::_is),
        symbol("eq?") to func(::eq)
)

