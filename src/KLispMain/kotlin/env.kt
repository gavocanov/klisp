package klisp

import kotlin.math.PI

val stdEnv: MutableMap<string, exp> = mutableMapOf(
        string("MAX_BYTE") to byte(Byte.MAX_VALUE),
        string("MAX_SHORT") to short(Short.MAX_VALUE),
        string("MAX_INT") to int(Int.MAX_VALUE),
        string("MAX_LONG") to long(Long.MAX_VALUE),
        string("MAX_FLOAT") to float(Float.MAX_VALUE),
        string("MAX_DOUBLE") to double(Double.MAX_VALUE),
        string("PI") to double(PI),
        string("true") to bool(true),
        string("false") to bool(false),
        string("begin") to func(::begin),
        string("*") to func(::mul),
        string("+") to func(::plus),
        string("-") to func(::minus),
        string("/") to func(::div),
        string(">") to func { it: List<exp> -> bool(compare(CompareOps.gt, it)) },
        string(">=") to func { it: List<exp> -> bool(compare(CompareOps.gte, it)) },
        string("=>") to func { it: List<exp> -> bool(compare(CompareOps.gte, it)) },
        string("<") to func { it: List<exp> -> bool(compare(CompareOps.lt, it)) },
        string("<=") to func { it: List<exp> -> bool(compare(CompareOps.lte, it)) },
        string("=<") to func { it: List<exp> -> bool(compare(CompareOps.lte, it)) },
        string("double?") to func { it: List<exp> -> isa(Types.double, it) },
        string("float?") to func { it: List<exp> -> isa(Types.float, it) },
        string("long?") to func { it: List<exp> -> isa(Types.long, it) },
        string("int?") to func { it: List<exp> -> isa(Types.int, it) },
        string("short?") to func { it: List<exp> -> isa(Types.short, it) },
        string("byte?") to func { it: List<exp> -> isa(Types.byte, it) },
        string("char?") to func { it: List<exp> -> isa(Types.char, it) },
        string("string?") to func { it: List<exp> -> isa(Types.string, it) },
        string("is?") to func(::_is),
        string("eq?") to func(::eq)
)

