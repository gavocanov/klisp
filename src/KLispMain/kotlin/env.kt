package klisp

import kotlin.math.PI

val stdEnv: MutableMap<symbol, exp> = mutableMapOf(
        symbol("pi") to float(PI.toFloat()),
        symbol("true") to bool(true),
        symbol("false") to bool(false),
        symbol("begin") to func(::begin),
        symbol("*") to func(::mul),
        symbol("+") to func(::plus),
        symbol("-") to func(::minus),
        symbol("/") to func(::div),
        symbol(">") to func(::gt),
        symbol(">=") to func(::gte),
        symbol("<") to func(::lt),
        symbol("<=") to func(::lte),
        symbol("string?") to func(::isString),
        symbol("is?") to func(::isa),
        symbol("eq?") to func(::eq)
)

