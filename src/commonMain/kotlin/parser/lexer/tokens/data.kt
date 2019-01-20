/**
 * Author: Matthew Might, translated to Kotlin by Paolo Gavocanov
 * Site:   http://matt.might.net/
 */

package klisp.parser.lexer.tokens

import klisp.parser.lexer.TokenTag

/**
 * Open brace tokens.
 */
data class OpenBraceToken(val value: String) : Token() {
    override val isParsingMarker: Boolean = false
    override val clazz: String = value
    override val tag: TokenTag = value

    override fun localCompare(other: Token): Int {
        other as OpenBraceToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "[$value]"
}

/**
 * Closing brace tokens.
 */
data class CloseBraceToken(val value: String) : Token() {
    override val isParsingMarker: Boolean = false
    override val clazz: String = value
    override val tag: TokenTag = value

    override fun localCompare(other: Token): Int {
        other as CloseBraceToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "[$value]"
}

/**
 * Symbol tokens.
 */
data class SymbolToken(val value: String) : Token() {
    companion object {
        const val tag: String = "Symbol"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as SymbolToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "'$value"
}

/**
 * String literal tokens.
 */
data class StringToken(val value: String) : Token() {
    companion object {
        const val tag: String = "String"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as StringToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "\"$value\""
}

/**
 * Integer tokens.
 */
data class IntToken(val value: Int) : Token() {
    companion object {
        const val tag: String = "Int"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as IntToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "$value"
}

/**
 * Boolean literal tokens.
 */
data class BooleanToken(val value: Boolean) : Token() {
    companion object {
        const val tag: String = "Boolean"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as BooleanToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = value.toString()
}

/**
 * Character tokens.
 */
data class CharToken(val value: Char) : Token() {
    companion object {
        const val tag: String = "Char"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as CharToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = "'$value'"
}

/**
 * Keyword tokens.
 */
data class KeywordToken(val value: String) : Token() {
    companion object {
        const val tag: String = "Keyword"
    }

    override val isParsingMarker: Boolean = false
    override val tag: TokenTag = Companion.tag

    override fun localCompare(other: Token): Int {
        other as KeywordToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = value
}

