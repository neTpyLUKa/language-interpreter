package org.solution

class Scope(private val values: MutableMap<String, Int> = mutableMapOf()) {
    var ifCount = 0
    fun getValue(identifier: String): Int {
        return values[identifier]!!
    }

    fun setValue(identifier: String, value: Int) {
        values[identifier] = value
    }
}

open class Object(val reader: Reader, val writer: Writer, val scope: Scope) {
    open fun eval(): Object? {
        throw RuntimeException("Base class method implementation should not be called")
    }
}

class Cell constructor(
    private val left: Object,
    private val right: Object,
    reader: Reader,
    writer: Writer,
    scope: Scope
) :
    Object(reader, writer, scope) {
    override fun eval(): Object? {
        left.eval()
        right.eval()
        return null
    }
}

open class Statement constructor(reader: Reader, writer: Writer, scope: Scope) : Object(reader, writer, scope) {
    override fun eval(): Object? {
        throw RuntimeException("Statement is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Statement {
            try {
                return IfStatement.parse(reader, writer, scope)
            } catch (e: Exception) {
                // trying other one
            }
            try {
                return AssignStatement.parse(reader, writer, scope)
            } catch (e: Exception) {
                // trying other one
            }

            try {
                return BlockStatement.parse(reader, writer, scope)
            } catch (e: Exception) {
                // trying other one
            }
            try {
                return ExpressionStatement.parse(reader, writer, scope)
            } catch (e: Exception) {
                throw RuntimeException("No statement candidate found")
            }
        }
    }
}

open class ExpressionStatement constructor(val expression: Expression, reader: Reader, writer: Writer, scope: Scope) :
    Statement(reader, writer, scope) {

    override fun eval(): Integer {
        val result = expression.eval()
        writer.append(result.value.toString())
        return result
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): ExpressionStatement {
            val expression = Expression.parse(reader, writer, scope)
            if (!reader.read(";")) {
                throw RuntimeException("Expected ;")
            }

            return ExpressionStatement(expression, reader, writer, scope)
        }
    }
}

class IfStatement constructor(
    private val expression: Expression?,
    private val statement: Statement,
    reader: Reader,
    writer: Writer,
    scope: Scope
) :
    Statement(reader, writer, scope) {
    override fun eval(): Object? {
        val result = expression?.eval()?.value
        if (result == null || result > 0) {
            return statement.eval()
        }
        return null
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): IfStatement {
            if (!reader.read("if")) {
                throw RuntimeException("Expected if")
            }
            val expression = Expression.parseInBrackets(reader, writer, scope)
            val statement = Statement.parse(reader, writer, scope)
            if (statement is BlockStatement) {
                val list = statement.statementList
                if (list is Cell) { // means there are two statements at least in if body
                    ++scope.ifCount
                }
            }
            return IfStatement(expression, statement, reader, writer, scope)
        }
    }
}

class AssignStatement constructor(
    private val identifier: Identifier,
    private val expression: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) : Statement(reader, writer, scope) {
    override fun eval(): Object? {
        val integer = expression.eval()
        val value = integer.value
        scope.setValue(identifier.identifier, value)
        return integer
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): AssignStatement {
            if (!reader.read("@")) {
                throw RuntimeException("Expected @")
            }
            val identifier = Identifier.parse(reader, writer, scope)
            if (!reader.read("=")) {
                throw RuntimeException("Expected @")
            }
            val expressionStatement = ExpressionStatement.parse(reader, writer, scope)
            return AssignStatement(identifier, expressionStatement.expression, reader, writer, scope)
        }
    }
}

class BlockStatement constructor(val statementList: Object, reader: Reader, writer: Writer, scope: Scope) :
    Statement(reader, writer, scope) {
    override fun eval(): Object? {
        return statementList.eval()
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): BlockStatement {
            if (!reader.read("{")) {
                throw RuntimeException("Expected {")
            }

            val statementList =
                parseStatementList(reader, writer, scope)
                    ?: throw RuntimeException("Empty block statement or error parsing")

            if (!reader.read("}")) {
                throw RuntimeException("Expected }")
            }
            return BlockStatement(statementList, reader, writer, scope)
        }
    }
}

open class Expression constructor(reader: Reader, writer: Writer, scope: Scope) :
    Statement(reader, writer, scope) {
    override fun eval(): Integer {
        throw RuntimeException("Expression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            return ConditionalExpression.parse(reader, writer, scope)
        }

        fun parseInBrackets(reader: Reader, writer: Writer, scope: Scope): Expression? {
            if (!reader.read("(")) {
                throw RuntimeException("Expected (")
            }

            val ans: Expression
            try {
                ans = parse(reader, writer, scope)
            } catch (e: Exception) {
                if (reader.read(")")) {
                    return null
                }
                throw RuntimeException("Expected )")
            }

            if (!reader.read(")")) {
                throw RuntimeException("Expected )")
            }

            return ans
        }
    }
}

open class ConditionalExpression constructor(reader: Reader, writer: Writer, scope: Scope) :
    Expression(reader, writer, scope) {
    override fun eval(): Integer {
        throw RuntimeException("Statement is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            val first = PlusMinusExpression.parse(reader, writer, scope)

            var isLess = false
            if (reader.read("<")) {
                isLess = true
            } else if (!reader.read(">")) {
                return first
            }
            val second = PlusMinusExpression.parse(reader, writer, scope)
            if (isLess) {
                return LessExpression(first, second, reader, writer, scope)
            }
            return GreaterExpression(first, second, reader, writer, scope)
        }
    }
}

class LessExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) : ConditionalExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        val toInt = if (leftValue < rightValue) 1 else 0
        return Integer(toInt, reader, writer, scope)
    }
}

class GreaterExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) : ConditionalExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        val toInt = if (leftValue > rightValue) 1 else 0
        return Integer(toInt, reader, writer, scope)
    }
}

open class PlusMinusExpression constructor(reader: Reader, writer: Writer, scope: Scope) :
    ConditionalExpression(reader, writer, scope) {
    override fun eval(): Integer {
        throw RuntimeException("PlusMinusExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            var pmExpr = MultDivExpression.parse(reader, writer, scope)
            while (true) {
                pmExpr = when {
                    reader.read("+") -> {
                        val mdExpr = MultDivExpression.parse(reader, writer, scope)
                        PlusExpression(pmExpr, mdExpr, reader, writer, scope)
                    }
                    reader.read("-") -> {
                        val mdExpr = MultDivExpression.parse(reader, writer, scope)
                        MinusExpression(pmExpr, mdExpr, reader, writer, scope)
                    }
                    else -> {
                        return pmExpr
                    }
                }
            }
        }
    }
}

class PlusExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) :
    PlusMinusExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return Integer(leftValue + rightValue, reader, writer, scope)
    }
}

class MinusExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) : PlusMinusExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return Integer(leftValue - rightValue, reader, writer, scope)
    }
}

open class MultDivExpression constructor(reader: Reader, writer: Writer, scope: Scope) :
    PlusMinusExpression(reader, writer, scope) {
    override fun eval(): Integer {
        throw RuntimeException("MultDivExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            var mdExpr = SimpleExpression.parse(reader, writer, scope)
            while (true) {
                mdExpr = when {
                    reader.read("*") -> {
                        val simpleExpr = SimpleExpression.parse(reader, writer, scope)
                        MultiplyExpression(mdExpr, simpleExpr, reader, writer, scope)
                    }
                    reader.read("/") -> {
                        val simpleExpr = SimpleExpression.parse(reader, writer, scope)
                        DivisionExpression(mdExpr, simpleExpr, reader, writer, scope)
                    }
                    else -> {
                        return mdExpr
                    }
                }
            }
        }
    }
}

class MultiplyExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) :
    MultDivExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return Integer(leftValue * rightValue, reader, writer, scope)
    }
}

class DivisionExpression constructor(
    private val left: Expression,
    private val right: Expression,
    reader: Reader,
    writer: Writer,
    scope: Scope
) :
    MultDivExpression(reader, writer, scope) {
    override fun eval(): Integer {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return Integer(leftValue / rightValue, reader, writer, scope)
    }
}

open class SimpleExpression constructor(reader: Reader, writer: Writer, scope: Scope) :
    MultDivExpression(reader, writer, scope) {
    override fun eval(): Integer {
        throw RuntimeException("SimpleExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            try {
                return Integer.parse(reader, writer, scope)
            } catch (e: Exception) {
                // trying other one
            }
            try {
                return Identifier.parse(reader, writer, scope)
            } catch (e: Exception) {
                // trying other one
            }
            try {
                return parseInBrackets(reader, writer, scope)!!
            } catch (e: Exception) {
                throw RuntimeException("No SimpleExpression candidate found")
            }
        }
    }
}

class Identifier constructor(val identifier: String, reader: Reader, writer: Writer, scope: Scope) :
    SimpleExpression(reader, writer, scope) {
    override fun eval(): Integer {
        return Integer(scope.getValue(identifier), reader, writer, scope)
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Identifier {
            val identifier = reader.readIdentifier() ?: throw RuntimeException("Error parsing identifier")
            return Identifier(identifier, reader, writer, scope)
        }
    }
}

class Integer constructor(val value: Int, reader: Reader, writer: Writer, scope: Scope) :
    SimpleExpression(reader, writer, scope) {
    override fun eval(): Integer {
        return this
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Integer {
            val integer = reader.readInteger() ?: throw RuntimeException("Error parsing integer")
            return Integer(integer, reader, writer, scope)
        }
    }
}

fun parseStatementList(reader: Reader, writer: Writer, scope: Scope): Object? {
    var stList: Object?
    try {
        stList = Statement.parse(reader, writer, scope)
    } catch (e: Exception) {
        return null
    }
    while (true) {
        var nextStatement: Statement?
        try {
            nextStatement = Statement.parse(reader, writer, scope)
        } catch (e: Exception) {
            return stList
        }

        stList = Cell(stList!!, nextStatement, reader, writer, scope)
    }
}

fun parseTree(code: String, writer: Writer): Pair<Object, Int> {
    val reader = Reader(code)
    val scope = Scope()
    val tmp = parseStatementList(reader, writer, scope)
    return Pair(tmp!!, scope.ifCount)
}

fun evalTree(obj: Object) {
    obj.eval()
}