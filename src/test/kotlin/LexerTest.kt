import klisp.parser.lexer.tokens.CharToken
import klisp.parser.lexer.tokens.DecToken
import klisp.parser.lexer.tokens.IntToken
import klisp.parser.lexer.tokens.KeywordToken
import klisp.parser.lexer.tokens.StringToken
import klisp.parser.lexer.tokens.SymbolToken
import kotlin.test.Test

@ExperimentalUnsignedTypes
class LexerTest {
    @Test
    fun integers() {
        "-1" shouldLexTo IntToken("-1")
        "1" shouldLexTo IntToken("1")

        "1e10" shouldFailToLexAs IntToken::class
        "1e-1" shouldFailToLexAs IntToken::class
    }

    @Test
    fun decimals() {
        "0.1" shouldLexTo DecToken("0.1")
        "-0.1" shouldLexTo DecToken("-0.1")
        "1000.10001" shouldLexTo DecToken("1000.10001")
    }

    @Test
    fun dotDecimals() {
        ".1" shouldLexTo DecToken(".1")
        "-.1" shouldLexTo DecToken("-.1")
        "-.00109" shouldLexTo DecToken("-.00109")
    }

    @Test
    fun expDecimals() {
        "-0.1e12" shouldLexTo DecToken("-0.1e12")
        "-100.1e-19" shouldLexTo DecToken("-100.1e-19")
    }

    @Test
    fun expDotDecimals() {
        "-.1e1" shouldLexTo DecToken("-.1e1")
        "-.1e-1" shouldLexTo DecToken("-.1e-1")
    }

    @Test
    fun keywords() {
        ":ab" shouldLexTo KeywordToken(":ab")
        ":a-b" shouldLexTo KeywordToken(":a-b")
        "a-b" shouldFailToLexAs KeywordToken::class
        "a-b:" shouldFailToLexAs KeywordToken::class
    }

    @Test
    fun strings() {
        """ "a b" """ shouldLexTo StringToken("a b")
    }

    @Test
    fun chars() {
        """ \n """ shouldLexTo CharToken('n')
    }

    @Test
    fun symbol() {
        """ n """ shouldLexTo SymbolToken("n")
        """ !kaka """ shouldLexTo SymbolToken("!kaka")
    }
}

