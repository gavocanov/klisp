@file:Suppress("ClassName")

package klisp

sealed class exp
sealed class atom : exp()
sealed class number : atom()

object unit : atom()
data class symbol(val value: String) : atom()
data class bool(val value: Boolean) : atom()
data class list(val value: List<exp>) : exp()
data class func(val func: (List<exp>) -> exp) : exp()
data class int(val value: Int) : number()
data class float(val value: Float) : number()

