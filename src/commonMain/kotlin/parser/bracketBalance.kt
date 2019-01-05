package klisp.parser

import klisp.Result
import klisp.data

/**
 * monoidal parsing from Edward Kmett (in haskell)
 * https://www.youtube.com/watch?v=Txf7swrcLYs&t=661s
 */
private typealias B = Pair<Int, Int>

typealias PP = Pair<Char, Char>

/**
 * semigroup for B
 */
private operator fun B.plus(that: B): B {
    val (l1, r1) = this
    val (l2, r2) = that
    return when {
        r1 <= l2 -> B(l1 + l2 - r1, r2)
        else -> B(l1, r2 + r1 - l2)
    }
}

/**
 * p.first is right bracket, p.second is left bracket
 * ie. inverted from what you'd imagine
 */
private fun parseBalanced(c: Char, p: PP): B {
    require(p.first != p.second) { "first and second can't be the same $p" }
    return when (c) {
        p.first -> B(0, 1)
        p.second -> B(1, 0)
        else -> B(0, 0)
    }
}

private fun addBalance(a: B, c: Char, p: PP): B =
        a + parseBalanced(c, p)

private fun addBalanceRound(a: B, c: Char) = addBalance(a, c, '(' to ')')
private fun addBalanceSquare(a: B, c: Char) = addBalance(a, c, '[' to ']')
private fun addBalanceCurly(a: B, c: Char) = addBalance(a, c, '[' to ']')
fun String.hasBalancedRoundBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceRound)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedSquareBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceSquare)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedCurlyBrackets(): Result<B> {
    val res = this.fold(B(0, 0), ::addBalanceCurly)
    return (res == B(0, 0)) to res
}

fun String.hasBalancedPairOf(p: PP): Result<B> {
    val res = this.fold(B(0, 0)) { a, n -> addBalance(a, n, p) }
    return (res == B(0, 0)) to res
}

val Result<B>.left: Int get() = this.data.second
val Result<B>.right: Int get() = this.data.first
