@file:Suppress("EXPERIMENTAL_API_USAGE")

package klisp

import kotlin.math.PI

val stdEnv: env = mutableMapOf(
    symbol("MAX_BYTE") to byte(Byte.MAX_VALUE),
    symbol("MAX_SHORT") to short(Short.MAX_VALUE),
    symbol("MAX_INT") to int(Int.MAX_VALUE),
    symbol("MAX_LONG") to long(Long.MAX_VALUE),
    symbol("MAX_UBYTE") to ubyte(UByte.MAX_VALUE),
    symbol("MAX_USHORT") to ushort(UShort.MAX_VALUE),
    symbol("MAX_UINT") to uint(UInt.MAX_VALUE),
    symbol("MAX_ULONG") to ulong(ULong.MAX_VALUE),
    symbol("MAX_FLOAT") to float(Float.MAX_VALUE),
    symbol("MAX_DOUBLE") to double(Double.MAX_VALUE),
    symbol("PI") to double(PI),
    symbol("pi") to double(PI),
    symbol("begin") to func { begin(it) },
    symbol("range") to func { range(it) },
    symbol("map") to func { map(it) },
    symbol("list") to func { list(it) },
    symbol("set") to func { set(it) },
    symbol("head") to func { (it.first() as collection).first() },
    symbol("first") to func { (it.first() as collection).first() },
    symbol("last") to func { (it.first() as collection).last() },
    symbol("car") to func { (it.first() as collection).first() },
    symbol("tail") to func { list((it.first() as collection).drop(1)) },
    symbol("rest") to func { list((it.first() as collection).drop(1)) },
    symbol("cdr") to func { list((it.first() as collection).drop(1)) },
    symbol("^") to func { foldableMathOp(mathOp.pow, it) },
    symbol("pow") to func { foldableMathOp(mathOp.pow, it) },
    symbol("%") to func { foldableMathOp(mathOp.rem, it) },
    symbol("rem") to func { foldableMathOp(mathOp.rem, it) },
    symbol("mod") to func { foldableMathOp(mathOp.rem, it) },
    symbol("abs") to func { foldableMathOp(mathOp.abs, it) },
    symbol("+") to func { foldableMathOp(mathOp.plus, it) },
    symbol("-") to func { foldableMathOp(mathOp.minus, it) },
    symbol("*") to func { foldableMathOp(mathOp.mul, it) },
    symbol("/") to func { foldableMathOp(mathOp.div, it) },
    symbol(">") to func { bool(compare(compareOp.gt, it)) },
    symbol(">=") to func { bool(compare(compareOp.gte, it)) },
    symbol("=>") to func { bool(compare(compareOp.gte, it)) },
    symbol("<") to func { bool(compare(compareOp.lt, it)) },
    symbol("<=") to func { bool(compare(compareOp.lte, it)) },
    symbol("=<") to func { bool(compare(compareOp.lte, it)) },
    symbol("=") to func { bool(compare(compareOp.eq, it)) },
    symbol("double?") to func { isa(type._double, it) },
    symbol("float?") to func { isa(type._float, it) },
    symbol("ulong?") to func { isa(type._ulong, it) },
    symbol("uint?") to func { isa(type._uint, it) },
    symbol("ushort?") to func { isa(type._ushort, it) },
    symbol("ubyte?") to func { isa(type._ubyte, it) },
    symbol("long?") to func { isa(type._long, it) },
    symbol("int?") to func { isa(type._int, it) },
    symbol("short?") to func { isa(type._short, it) },
    symbol("byte?") to func { isa(type._byte, it) },
    symbol("char?") to func { isa(type._char, it) },
    symbol("string?") to func { isa(type._string, it) },
    symbol("keyword?") to func { isa(type._keyword, it) },
    symbol("list?") to func { isa(type._list, it) },
    symbol("map?") to func { isa(type._map, it) },
    symbol("set?") to func { isa(type._set, it) },
    symbol("collection?") to func { isa(type._collection, it) },
    symbol("number?") to func { isa(type._number, it) },
    symbol("integer?") to func { isa(type._integer, it) },
    symbol("decimal?") to func { isa(type._decimal, it) },
    symbol("atom?") to func { isa(type._atom, it) },
    symbol("bool?") to func { isa(type._bool, it) },
    symbol("is?") to func { _is(it) },
    symbol("json") to func { json(it) },
    symbol("eq?") to func { eq(it) },
    symbol("equal?") to func { eq(it) },
    symbol("lex") to func { lex(it) }
).also { it.map { (k, v) -> v.meta = string(k.value) } }

