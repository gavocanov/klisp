import klisp.byte
import klisp.char
import klisp.double
import klisp.float
import klisp.int
import klisp.keyword
import klisp.long
import klisp.short
import klisp.string
import klisp.ubyte
import klisp.uint
import klisp.ulong
import klisp.ushort
import kotlinx.serialization.json.JSON
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import kotlin.test.Test

@ExperimentalUnsignedTypes
class JsonTest {
    @Test
    fun `atoms to json`() {
        byte(1).toJson() shouldEqual _eval("1").toJson()
        ubyte(1u).toJson() shouldEqual _eval("1").toJson()
        short(1).toJson() shouldEqual _eval("1").toJson()
        ushort(1u).toJson() shouldEqual _eval("1").toJson()
        int(1).toJson() shouldEqual _eval("1").toJson()
        uint(1u).toJson() shouldEqual _eval("1").toJson()
        long(1).toJson() shouldEqual _eval("1").toJson()
        ulong(1u).toJson() shouldEqual _eval("1.0").toJson()
        float(1.0f).toJson() shouldEqual _eval("1.0").toJson()
        double(1.0).toJson() shouldEqual _eval("1.0").toJson()
        char('1').toJson() shouldEqual _eval("\\1").toJson()
        string("\"1\"").toJson() shouldEqual _eval("\"1\"").toJson()
        keyword(":a").toJson() shouldEqual _eval(":a").toJson()
    }

    @Test
    fun `collections to json`() {
        "[1]" shouldEqual _eval("(list 1)").toJson()
        "[1]" shouldEqual _eval("(set 1)").toJson()
        "[true,false]" shouldEqual _eval("(list true false)").toJson()
        "[true,false]" shouldEqual _eval("(set true false)").toJson()
        JSON.stringify(String.serializer().list, listOf(JSON.stringify(String.serializer(), "a"), "a")) shouldEqual _eval("(list \"a\" \\a)").toJson()
        JSON.stringify(String.serializer().list, listOf(JSON.stringify(String.serializer(), "a"), "a")) shouldEqual _eval("(set \"a\" \\a)").toJson()
    }

    @Test
    fun `maps to json`() {
        """{"a":1}""" shouldEqual _eval("(map :a 1)").toJson()
        """{"a":["\"a\"",1,true]}""" shouldEqual _eval("(map :a (list \"a\" 1 true))").toJson()

        """{"list-1":[1,1.0,true,false,["\"str\"","c"]],"map-A":{"TesT":[1]}}""" shouldEqual
                _eval("""
            (map :list-1 (list 1 1.0 true false (list "str" \c))
                 :map-A (map :TesT (list 1)))
        """.trimIndent()).toJson()
    }

    @Test
    fun `strings to json`() {
        JSON.stringify(String.serializer(), "\"1\"") shouldEqual _eval("\"1\"").toJson()
        JSON.stringify(String.serializer(), "\"i love my 'shoes'\"") shouldEqual _eval(""" "i love my 'shoes'" """).toJson()
        // TODO
//        JSON.stringify(""""bu""") shouldEqual _eval(""" ""bu" """).toJson()
    }
}
