import klisp.*
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class AtomsTest {
    @Test
    fun bool() {
        "true" shouldEvalTo true
        "false" shouldEvalTo false
    }

    @Test
    fun byte() {
        assertEquals(byte(Byte.MAX_VALUE), _eval("${Byte.MAX_VALUE}"), "byte max")
        assertEquals(byte(Byte.MIN_VALUE), _eval("${Byte.MIN_VALUE}"), "byte min")
        assertEquals(ubyte(UByte.MAX_VALUE), _eval("${UByte.MAX_VALUE}"), "ubyte max")
        assertEquals(byte(0), _eval("${UByte.MIN_VALUE}"), "ubyte min")
    }

    @Test
    fun short() {
        assertEquals(short(Short.MAX_VALUE), _eval("${Short.MAX_VALUE}"), "short max")
        assertEquals(short(Short.MIN_VALUE), _eval("${Short.MIN_VALUE}"), "short min")
        assertEquals(ushort(UShort.MAX_VALUE), _eval("${UShort.MAX_VALUE}"), "ushort max")
        assertEquals(byte(0), _eval("${UShort.MIN_VALUE}"), "ushort min")
    }

    @Test
    fun int() {
        assertEquals(int(Int.MAX_VALUE), _eval("${Int.MAX_VALUE}"), "int max")
        assertEquals(int(Int.MIN_VALUE), _eval("${Int.MIN_VALUE}"), "int min")
        assertEquals(uint(UInt.MAX_VALUE), _eval("${UInt.MAX_VALUE}"), "uint max")
        assertEquals(byte(0), _eval("${UInt.MIN_VALUE}"), "uint min")
    }

    @Test
    fun long() {
        assertEquals(long(Long.MAX_VALUE), _eval("${Long.MAX_VALUE}"), "long max")
        assertEquals(long(Long.MIN_VALUE), _eval("${Long.MIN_VALUE}"), "long min")
        assertEquals(ulong(ULong.MAX_VALUE), _eval("${ULong.MAX_VALUE}"), "ulong max")
        assertEquals(byte(0), _eval("${ULong.MIN_VALUE}"), "ulong min")
    }

    @Test
    fun float() {
        assertEquals(float(Float.MAX_VALUE), _eval("${Float.MAX_VALUE}"), "float max")
        assertEquals(float(Float.MIN_VALUE), _eval("${Float.MIN_VALUE}"), "float min")
        assertEquals(float(0.0f), _eval("0.0"), "float zero")
    }

    @Test
    fun double() {
        assertEquals(double(Double.MAX_VALUE), _eval("${Double.MAX_VALUE}"), "double max")
        assertEquals(double(Double.MIN_VALUE), _eval("${Double.MIN_VALUE}"), "double min")
        assertEquals(double(0.0).value.toFloat(), (_eval("0.0") as float).value, "double zero")
    }

    @Test
    fun string() = assertEquals(string("\"111 aaa\""), _eval("\"111 aaa\""), "string")

    @Test
    fun char() = assertEquals(char('1'), _eval("\\1"), "char")

    @Test
    fun set() = assertEquals(set(listOf(byte(1), byte(1), byte(2))), _eval("(set 1 2)"), "set")

    @Test
    fun list() = assertEquals(list(listOf(byte(1))), _eval("(list 1)"), "list")

    @Test
    fun keyword() = assertEquals(keyword(":a"), _eval(":a"), "keyword")

    @Test
    fun symbol() = assertEquals(double(PI), _eval("pi"), "symbol")

    @Test
    fun map() = assertEquals(map(listOf(keyword(":a"), byte(1))), _eval("(map :a 1)"), "map")

    @Test
    fun mix() {
        val a = _eval("""
            (map    :byte 1
                    :float 1.0
                    :double -1e103
                    :string "11 aa"
                    :char \1
                    :list (list 1 1.0 (map :a 1) :key)
                    :set (set 1 1 1 1 2 2 2 2)
                    :map (map :a (list 1 2 3 true))
                    :true true
                    :false false)
        """.trimIndent())
        val e = map(mapOf(
                keyword(":byte") to byte(1),
                keyword(":float") to float(1.0f),
                keyword(":double") to double(-1e103),
                keyword(":string") to string(""""11 aa""""),
                keyword(":char") to char('1'),
                keyword(":list") to list(listOf(
                        byte(1),
                        float(1.0f),
                        map(mapOf(keyword(":a") to byte(1))),
                        keyword(":key")
                )),
                keyword(":set") to set(listOf(
                        byte(1),
                        byte(2)
                )),
                keyword(":map") to map(mapOf(
                        keyword(":a") to list(listOf(
                                byte(1),
                                byte(2),
                                byte(3),
                                bool(true)
                        ))
                )),
                keyword(":true") to bool(true),
                keyword(":false") to bool(false)
        ))
        assertEquals(e, a, "mix")
    }
}