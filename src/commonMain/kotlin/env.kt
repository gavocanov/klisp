package klisp

import kotlin.math.PI

val stdEnv: env = mutableMapOf(
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
        symbol("begin") to func { begin(it) },
        symbol("range") to func { range(it) },
        symbol("list") to func { list(it) },
        symbol("set") to func { set(it) },
        symbol("head") to func { it -> (it.first() as list).first() },
        symbol("first") to func { it -> (it.first() as list).first() },
        symbol("last") to func { it -> (it.first() as list).last() },
        symbol("car") to func { it -> (it.first() as list).first() },
        symbol("tail") to func { it -> list((it.first() as list).drop(1)) },
        symbol("rest") to func { it -> list((it.first() as list).drop(1)) },
        symbol("cdr") to func { it -> list((it.first() as list).drop(1)) },
        symbol("+") to func { foldableMathOp(mathOp.plus, it) },
        symbol("-") to func { foldableMathOp(mathOp.minus, it) },
        symbol("*") to func { foldableMathOp(mathOp.mul, it) },
        symbol("/") to func { foldableMathOp(mathOp.div, it) },
        symbol(">") to func { it: exps -> bool(compare(compareOp.gt, it)) },
        symbol(">=") to func { it: exps -> bool(compare(compareOp.gte, it)) },
        symbol("=>") to func { it: exps -> bool(compare(compareOp.gte, it)) },
        symbol("<") to func { it: exps -> bool(compare(compareOp.lt, it)) },
        symbol("<=") to func { it: exps -> bool(compare(compareOp.lte, it)) },
        symbol("=<") to func { it: exps -> bool(compare(compareOp.lte, it)) },
        symbol("double?") to func { it: exps -> isa(type.double, it) },
        symbol("float?") to func { it: exps -> isa(type.float, it) },
        symbol("long?") to func { it: exps -> isa(type.long, it) },
        symbol("int?") to func { it: exps -> isa(type.int, it) },
        symbol("short?") to func { it: exps -> isa(type.short, it) },
        symbol("byte?") to func { it: exps -> isa(type.byte, it) },
        symbol("char?") to func { it: exps -> isa(type.char, it) },
        symbol("string?") to func { it: exps -> isa(type.string, it) },
        symbol("keyword?") to func { it: exps -> isa(type.keyword, it) },
        symbol("list?") to func { it: exps -> isa(type.list, it) },
        symbol("set?") to func { it: exps -> isa(type.set, it) },
        symbol("collection?") to func { it: exps -> isa(type.collection, it) },
        symbol("number?") to func { it: exps -> isa(type.number, it) },
        symbol("integer?") to func { it: exps -> isa(type.integer, it) },
        symbol("decimal?") to func { it: exps -> isa(type.decimal, it) },
        symbol("symbol?") to func { it: exps -> isa(type.symbol, it) },
        symbol("atom?") to func { it: exps -> isa(type.atom, it) },
        symbol("is?") to func { _is(it) },
        symbol("eq?") to func { eq(it) },
        symbol("equal?") to func { eq(it) }
).also { it.map { (k, v) -> v.meta = k } }

