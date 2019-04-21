/**
 * monoidal parsing from Edward Kmett (in haskell)
 * https://www.youtube.com/watch?v=Txf7swrcLYs&t=661s
 */

@file:Suppress("unused")

package klisp.parser

import klisp.Result
import klisp.data

/**
 * semigroup for B
 */
private operator fun Pair<Int, Int>.plus(that: Pair<Int, Int>): Pair<Int, Int> {
    val (l1, r1) = this
    val (l2, r2) = that
    return when {
        r1 <= l2 -> Pair(l1 + l2 - r1, r2)
        else -> Pair(l1, r2 + r1 - l2)
    }
}

/**
 * p.first is right bracket, p.second is left bracket
 * ie. inverted from what you'd imagine
 */
private fun parseBalanced(c: Char, p: Pair<Char, Char>): Pair<Int, Int> {
    require(p.first != p.second) { "first and second can't be the same $p" }
    return when (c) {
        p.first -> Pair(0, 1)
        p.second -> Pair(1, 0)
        else -> Pair(0, 0)
    }
}

private fun addBalance(a: Pair<Int, Int>, c: Char, p: Pair<Char, Char>): Pair<Int, Int> = a + parseBalanced(c, p)
private fun addBalanceRound(a: Pair<Int, Int>, c: Char) = addBalance(a, c, '(' to ')')
private fun addBalanceSquare(a: Pair<Int, Int>, c: Char) = addBalance(a, c, '[' to ']')
private fun addBalanceCurly(a: Pair<Int, Int>, c: Char) = addBalance(a, c, '[' to ']')

fun String.hasBalancedRoundBrackets(): Result<Pair<Int, Int>> {
    val res = this.fold(Pair(0, 0), ::addBalanceRound)
    return (res == Pair(0, 0)) to res
}

fun String.hasBalancedSquareBrackets(): Result<Pair<Int, Int>> {
    val res = this.fold(Pair(0, 0), ::addBalanceSquare)
    return (res == Pair(0, 0)) to res
}

fun String.hasBalancedCurlyBrackets(): Result<Pair<Int, Int>> {
    val res = this.fold(Pair(0, 0), ::addBalanceCurly)
    return (res == Pair(0, 0)) to res
}

fun String.hasBalancedPairOf(p: Pair<Char, Char>): Result<Pair<Int, Int>> {
    val res = this.fold(Pair(0, 0)) { a, n -> addBalance(a, n, p) }
    return (res == Pair(0, 0)) to res
}

inline val Result<Pair<Int, Int>>.left: Int get() = this.data.second
inline val Result<Pair<Int, Int>>.right: Int get() = this.data.first
