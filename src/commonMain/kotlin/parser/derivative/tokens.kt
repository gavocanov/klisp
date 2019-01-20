package klisp.parser.derivative

/**
 * A token tag indicates the "type" of a token.
 *
 * For tokens generated by a lexer, it's appropriate to use a string.
 *
 * For example: "ID", "INT", "LPAREN", "SEMICOLON"
 *
 * There should be a finite number of token tags, so "ID(foo)" is a bad
 * token tag.
 *
 * During the parsing process, special tokens called parsing markers may
 * take control of this field.
 */
internal typealias TokenTag = Any

/**
 * Parsers consume live streams of tokens (or objects that can be implicitly converted into them).
 *
 * A token is an individual lexeme.
 *
 * (In terms of context-free grammars, tokens are the terminals.)
 */
abstract class Token : Comparable<Token> {
    /**
     * @return true if this token represents a parsing marker.
     *
     * Parsing markers are special tokens that do not appear in input
     * strings; they only appear in parse strings to indicate the
     * structure of the parse.
     */
    abstract val isParsingMarker: Boolean

    /**
     * The class of this token.
     */
    protected open val clazz: String
        get() = this::class.toString()

    /**
     * The tag of this token.
     *
     * A token's tag indicates to which lexical class it belongs.
     *
     * Tokens consumed as input should have strings for their tags.
     *
     * Examples of good tags would be, "Identifier", "Integer", "String", ";", "(", ")".
     *
     * Parsing markers will have special tags.
     */
    abstract val tag: TokenTag

    protected abstract fun localCompare(other: Token): Int

    override fun compareTo(other: Token): Int {
        val c1 = clazz.compareTo(other.clazz)
        if (c1 != 0)
            return c1
        return localCompare(other)
    }
}

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
 * Punctuation tokens.
 */
data class PunctToken(val value: String) : Token() {
    override val isParsingMarker: Boolean = false
    override val clazz: String = value
    override val tag: TokenTag = value

    override fun localCompare(other: Token): Int {
        other as PunctToken
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
    override val tag: TokenTag = SymbolToken.tag

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
    override val tag: TokenTag = StringToken.tag

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
    override val tag: TokenTag = IntToken.tag

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
    override val tag: TokenTag = BooleanToken.tag

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
    override val tag: TokenTag = CharToken.tag

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
    override val tag: TokenTag = KeywordToken.tag

    override fun localCompare(other: Token): Int {
        other as KeywordToken
        return value.compareTo(other.value)
    }

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?) = value == other
    override fun toString(): String = value
}

