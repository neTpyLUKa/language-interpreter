import org.junit.Test as test
import org.solution.Writer
import org.solution.evalTree
import org.solution.parseTree

class TestWriter : Writer() {
    private var first = true
    override fun append(string: String) {
        if (first) {
            first = false
        } else {
            sb.append(", ")
        }
        sb.append(string)

    }

    fun getString(): String {
        return sb.toString()
    }
}

data class TestCase(val input: String, val expected: String, val name: String)

class TestInterpreter {
    @test(expected = Exception::class)
    fun empty() {
        val testWriter = TestWriter()
        parseTree("", testWriter)
    }

    private val testsCorrect =
        listOf(
            TestCase("3+4;", "7", "simple"),
            TestCase("1 - 2; 6 * 3;", "-1, 18", "twoStatements"),
            TestCase("@x = 3 / 1; x * 2 + 1;", "7", "assignment"),
            TestCase("if (1 + 2) 3 * -1 + 4; 123+3   \n + 43;", "1, 169", "ifStatement"),
            TestCase("if (1) { 6 * 3; \n\n 3 * (1 - 2); }", "18, -3", "ifWithBlock"),
            TestCase("(3 * 7 + 5 * 6) / (-2 * -9 - (-3 - 4));", "2", "complex expression"),
            TestCase("if (-1 < -2) 1 / 0;", "", "no executing if false branch"),
            TestCase("if (3 + 2 < 6) @x123_ = 42; x123_;", "42", "variables in global scope"),
            TestCase("@y = (1 > 0) + (2 > 1) - (3 < 4); y + 1;", "2", "less and greater"),
            TestCase("{3;{4;{5;}}}", "3, 4, 5", "nested blocks"),
            TestCase("1;2;3;4;5;6;7;8;9;10;11;", "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11", "long"),
            TestCase(" @ x = 3 ; @ x  =  4 ;  x ;", "4", "reassignment")
        )

    @test
    fun runCorrect() {
        for (test in testsCorrect) {
            try {
                val testWriter = TestWriter()
                val tree = parseTree(test.input, testWriter)
                evalTree(tree)
                assert(testWriter.getString() == test.expected) { "TEST FAILED: ${test.name}, ${testWriter.getString()} != ${test.expected}" }
            } catch (e: Exception) {
                assert(false) {
                    "TEST FAILED, UNEXPECTED EXCEPTION:  ${test.name}, ${e.message}"
                }
            }
        }
    }

    private val testsThrows = listOf(
        TestCase("3", "", "no ; in end"),
        TestCase("@a=3;b;", "", "not set variable"),
        TestCase("if 1 2;", "", "no brackets in if condition"),
        TestCase("3 / 0;", "", "division by zero"),
        TestCase("@x=x;", "", "recursively init"),
        TestCase("{3=4;}", "", "= not in initialization"),
        TestCase("--4;", "", "Wrong int"),
        TestCase("@asdsa$ = 123;", "", "wrong identifier"),
        TestCase("if (3 + 2 > 6) @x123_ = 42; x123_;", "", "variables in global scope"),
        TestCase("1; 2; 3", "", "no comma"),
        TestCase("1;2; if(1) {2; 3}", "", "no comma in st block"),
        TestCase("@1x = 4; 3 + 3;", "", "wrong identifier")
    )

    @test
    fun runIncorrect() {
        for (test in testsThrows) {
            val testWriter = TestWriter()
            try {
                val tree = parseTree(test.input, testWriter)
                evalTree(tree)
                assert(false) { test.name }
            } catch (e: Exception) {
                // ok
            }
        }
    }
}