import org.solution.Reader
import org.junit.Test as test

class ReaderTests {
    @test
    fun empty() {
        val reader = Reader("")
        assert(!reader.read("asd"))
        assert(reader.readInteger() == null)
        assert(reader.readIdentifier() == null)
    }

    @test
    fun read() {
        var reader = Reader("asd")
        assert(reader.read("asd"))
        assert(!reader.read("a"))

        reader = Reader("aba caba")
        assert(reader.read("aba"))
        assert(reader.read("caba"))

        reader = Reader("    abc     ")
        assert(reader.read("a"))
        assert(reader.read("b"))
    }

    @test
    fun readIdentifier() {
        var reader = Reader("   @hello  ")
        assert(reader.read("@"))
        assert(reader.readIdentifier() == "hello")

        reader = Reader(" 1az  *as _1QWE          long_identifier1243 id+      ^")
        assert(reader.readIdentifier() == null)
        assert(reader.read("1az"))

        assert(reader.readIdentifier() == null)
        assert(reader.read("*as"))

        assert(reader.readIdentifier() == "_1QWE")
        assert(!reader.read("_1QWE"))

        assert(reader.readIdentifier() == "long_identifier1243")

        assert(reader.readIdentifier() == "id")
        assert(reader.read("+"))

        assert(reader.readIdentifier() == null)

        for (ch in 'a'..'z') {
            assert(!reader.read(ch.toString()))
        }

        for (ch in 'A'..'Z') {
            assert(!reader.read(ch.toString()))
        }

        for (ch in '0'..'9') {
            assert(!reader.read(ch.toString()))
        }
    }

    @test
    fun readInteger() {
        var reader = Reader("   @1a3232_0000  ")
        assert(reader.readInteger() == null)

        assert(reader.read("@"))
        assert(reader.readInteger() == 1)

        assert(reader.read("a"))
        assert(reader.readInteger() == 3232)

        assert(reader.read("_"))
        assert(reader.readInteger() == 0)

        assert(reader.readInteger() == null)

        reader = Reader("+85553535 -01 -23+654 ++1")

        assert(reader.readInteger() == 85553535)
        assert(reader.readInteger() == -1)
        assert(reader.readInteger() == -23)
        assert(reader.readInteger() == 654)

        assert(reader.readInteger() == null)
        assert(reader.read("++1"))
    }

    @test
    fun something() {
        val reader = Reader("{3;}")
        assert(reader.read("{"))
        assert(reader.read("3"))
        assert(reader.read(";"))
        assert(reader.read("}"))
    }
}
