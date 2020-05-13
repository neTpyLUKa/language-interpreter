package org.solution

class SyntaxException(message: String) : Exception(message)

class Scope(private val values: MutableMap<String, Int> = mutableMapOf()) {
    fun getValue(identifier: String): Int {
        return values[identifier]!!
    }

    fun setValue(identifier: String, value: Int) {
        values[identifier] = value
    }
}

enum class CompareResult {
    NewIfFound, IfNotFound, OtherChangeDetected
}

open class BaseObject {
    open fun eval(): BaseObject? {
        throw RuntimeException("Base class method implementation should not be called")
    }

    open fun compareWithPrev(tree: BaseObject): CompareResult { // return true if new if detected
        throw RuntimeException("Base class method implementation should not be called")
    }
}

class Cell constructor(
    private val left: BaseObject,
    private val right: BaseObject
) :
    BaseObject() {
    override fun eval(): BaseObject? {
        left.eval()
        right.eval()
        return null
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Cell cmp")
        if (tree is Cell) {
            //    println("Cell right cmp")
            val rhs = right.compareWithPrev(tree.right)
            if (rhs != CompareResult.IfNotFound) {
                return rhs
            }
            //  println("Cell left cmp")
            return left.compareWithPrev(tree.left)
        }
        return CompareResult.OtherChangeDetected
    }
}

open class Statement : BaseObject() {
    override fun eval(): BaseObject? {
        throw RuntimeException("Statement is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("Statement is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Statement? {
            val ch = reader.peek() ?: return null
            //  println("ch = $ch")
            when (ch) {
                'i' -> {
                    return IfStatement.parse(reader, writer, scope)
                }
                '@' -> {
                    return AssignStatement.parse(reader, writer, scope)
                }
                '{' -> {
                    return BlockStatement.parse(reader, writer, scope)
                }
                else -> {
                    val oldPos = reader.pos
                    return try {
                        ExpressionStatement.parse(reader, writer, scope)
                    } catch (e: Exception) {
                        reader.pos = oldPos
                        null
                    }
                }
            }
        }
    }
}

open class ExpressionStatement constructor(val expression: Expression, private val writer: Writer) :
    Statement() {

    override fun eval(): IntExpression {
        val result = expression.eval()
        writer.append(result.value.toString())
        return result
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("ExprSt cmp")
        if (tree is ExpressionStatement) {
            //    println("ExprSt Expr cmp")
            return expression.compareWithPrev(tree.expression)
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): ExpressionStatement {
            val expression = Expression.parse(reader, writer, scope)

            if (!reader.read(";")) {
                throw SyntaxException("Expected ;")
            }

            return ExpressionStatement(expression, writer)
        }
    }
}

class IfStatement constructor(
    private val expression: Expression?,
    private val statement: Statement
) :
    Statement() {
    override fun eval(): BaseObject? {
        val result = expression?.eval()?.value
        if (result == null || result > 0) {
            return statement.eval()
        }
        return null
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        // println("If cmp")
        val curType = getIfType()
        if (tree is IfStatement) {
            val prevType = tree.getIfType()
            if (curType == IfType.TwoOrMoreStatement && prevType == IfType.OneStatement) {
                return CompareResult.NewIfFound
            }
            //   println("If st cmp")
            return statement.compareWithPrev(tree.statement)
        }
        if (curType == IfType.TwoOrMoreStatement) {
            return CompareResult.NewIfFound
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): IfStatement {
            if (!reader.read("if")) {
                throw SyntaxException("Expected if")
            }

            val expression = Expression.parseInBrackets(reader, writer, scope)

            val statement =
                Statement.parse(reader, writer, scope) ?: throw SyntaxException("Error parsing statement (if)")

            return IfStatement(expression, statement)
        }
    }

    fun getIfType(): IfType {
        if (statement is BlockStatement) {
            val list = statement.statementList
            if (list is Cell) { // means there are two statements at least in if body
                return IfType.TwoOrMoreStatement
            }
        }
        return IfType.OneStatement
    }

    enum class IfType {
        OneStatement, TwoOrMoreStatement
    }
}

class AssignStatement constructor(
    private val identifier: Identifier,
    private val expression: Expression,
    private val scope: Scope
) : Statement() {
    override fun eval(): BaseObject? {
        val integer = expression.eval()
        val value = integer.value
        scope.setValue(identifier.identifier, value)
        return integer
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Assign cmp")
        if (tree is AssignStatement) {
            //    println("Assign Id cmp")
            val idCmp = identifier.compareWithPrev(tree.identifier)
            if (idCmp != CompareResult.IfNotFound) {
                return idCmp
            }
            //  println("Assign Expr cmp")
            return expression.compareWithPrev(tree.expression)
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): AssignStatement {
            if (!reader.read("@")) {
                throw SyntaxException("Expected @")
            }

            val identifier = Identifier.parse(reader, scope)

            if (!reader.read("=")) {
                throw SyntaxException("Expected =")
            }

            val expressionStatement = ExpressionStatement.parse(reader, writer, scope)

            return AssignStatement(identifier, expressionStatement.expression, scope)
        }
    }
}

class BlockStatement constructor(val statementList: BaseObject) :
    Statement() {
    override fun eval(): BaseObject? {
        return statementList.eval()
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Block cmp")
        if (tree is BlockStatement) {
            //    println("Block StList cmp")
            return statementList.compareWithPrev(tree.statementList)
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): BlockStatement {
            if (!reader.read("{")) {
                throw SyntaxException("Expected {")
            }

            val statementList = parseStatementList(reader, writer, scope)!!

            if (!reader.read("}")) {
                throw SyntaxException("Expected }")
            }

            return BlockStatement(statementList)
        }
    }
}

open class Expression :
    Statement() {
    override fun eval(): IntExpression {
        throw RuntimeException("Expression is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("Expression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            return ConditionalExpression.parse(reader, writer, scope)
        }

        fun parseInBrackets(reader: Reader, writer: Writer, scope: Scope): Expression? {
            if (!reader.read("(")) {
                throw SyntaxException("Expected (")
            }
            if (reader.read(")")) {
                return null
            }

            val ans = parse(reader, writer, scope)

            if (!reader.read(")")) {
                throw SyntaxException("Expected )")
            }
            return ans
        }
    }
}

open class ConditionalExpression :
    Expression() {
    override fun eval(): IntExpression {
        throw RuntimeException("Statement is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("Statement is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            // println("Parsing condExpr")
            val first = PlusMinusExpression.parse(reader, writer, scope)

            var isLess = false
            if (reader.read("<")) {
                isLess = true
            } else if (!reader.read(">")) {
                return first
            }
            val second = PlusMinusExpression.parse(reader, writer, scope)
            if (isLess) {
                return LessExpression(first, second)
            }
            return GreaterExpression(first, second)
        }
    }
}

class LessExpression constructor(
    private val left: Expression,
    private val right: Expression
) : ConditionalExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        val toInt = if (leftValue < rightValue) 1 else 0
        return IntExpression(toInt)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Less cmp")
        if (tree is LessExpression) {
            //    println("Less left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //  println("Less right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

class GreaterExpression constructor(
    private val left: Expression,
    private val right: Expression
) : ConditionalExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        val toInt = if (leftValue > rightValue) 1 else 0
        return IntExpression(toInt)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //   println("Greater cmp")
        if (tree is GreaterExpression) {
            //     println("Greater left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //   println("Greater right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

open class PlusMinusExpression :
    ConditionalExpression() {
    override fun eval(): IntExpression {
        throw RuntimeException("PlusMinusExpression is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("PlusMinusExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            // println("Parsing pmexpr")
            var pmExpr = MultDivExpression.parse(reader, writer, scope)

            while (true) {
                pmExpr = when {
                    reader.read("+") -> {
                        val mdExpr = MultDivExpression.parse(reader, writer, scope)
                        PlusExpression(pmExpr, mdExpr)
                    }

                    reader.read("-") -> {
                        val mdExpr = MultDivExpression.parse(reader, writer, scope)
                        MinusExpression(pmExpr, mdExpr)
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
    private val right: Expression
) :
    PlusMinusExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return IntExpression(leftValue + rightValue)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Plus cmp")
        if (tree is PlusExpression) {
            //    println("Plus left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //  println("Plus right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

class MinusExpression constructor(
    private val left: Expression,
    private val right: Expression
) : PlusMinusExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return IntExpression(leftValue - rightValue)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Minus cmp")
        if (tree is MinusExpression) {
            //    println("Minus left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //  println("Minus right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

open class MultDivExpression :
    PlusMinusExpression() {
    override fun eval(): IntExpression {
        throw RuntimeException("MultDivExpression is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("MultDivExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            //  println("Parsing MD expr")
            var mdExpr = SimpleExpression.parse(reader, writer, scope)

            while (true) {
                mdExpr = when {
                    reader.read("*") -> {
                        val simpleExpr = SimpleExpression.parse(reader, writer, scope)
                        MultiplyExpression(mdExpr, simpleExpr)
                    }

                    reader.read("/") -> {
                        val simpleExpr = SimpleExpression.parse(reader, writer, scope)
                        DivisionExpression(mdExpr, simpleExpr)
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
    private val right: Expression
) :
    MultDivExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return IntExpression(leftValue * rightValue)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Mult cmp")
        if (tree is MultiplyExpression) {
            //    println("Mult left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //  println("Mult right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

class DivisionExpression constructor(
    private val left: Expression,
    private val right: Expression
) :
    MultDivExpression() {
    override fun eval(): IntExpression {
        val leftValue = left.eval().value
        val rightValue = right.eval().value
        return IntExpression(leftValue / rightValue)
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //    println("Div cmp")
        if (tree is DivisionExpression) {
            //      println("Div left Expr cmp")
            val lhs = left.compareWithPrev(tree.left)
            if (lhs != CompareResult.IfNotFound) {
                return lhs
            }
            //    println("Div right Expr cmp")
            return right.compareWithPrev(tree.right)
        }
        return CompareResult.OtherChangeDetected
    }
}

open class SimpleExpression :
    MultDivExpression() {
    override fun eval(): IntExpression {
        throw RuntimeException("SimpleExpression is not for explicit construction")
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        throw RuntimeException("SimpleExpression is not for explicit construction")
    }

    companion object {
        fun parse(reader: Reader, writer: Writer, scope: Scope): Expression {
            //  println("Parsing Simple expr")
            val ch = reader.peek() ?: throw SyntaxException("EOF")
            //  println("ch = $ch")

            if (ch == '(') {
                return parseInBrackets(reader, writer, scope)!!
            }

            try {
                return IntExpression.parse(reader)
            } catch (e: SyntaxException) {
                // trying other one. integer failure does not change reader state
            }

            return Identifier.parse(reader, scope)
        }
    }
}

class Identifier constructor(val identifier: String, private val scope: Scope) :
    SimpleExpression() {
    override fun eval(): IntExpression {
        return IntExpression(scope.getValue(identifier))
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //  println("Id cmp")
        if (tree is Identifier) {
            if (identifier != tree.identifier) {
                //        println("Id not equal")
                return CompareResult.OtherChangeDetected
            }
            //  println("Id equal")
            return CompareResult.IfNotFound
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader, scope: Scope): Identifier {
            val identifier = reader.readIdentifier() ?: throw SyntaxException("Error parsing identifier")
            return Identifier(identifier, scope)
        }
    }
}

class IntExpression constructor(val value: Int) :
    SimpleExpression() {
    override fun eval(): IntExpression {
        return this
    }

    override fun compareWithPrev(tree: BaseObject): CompareResult {
        //   println("Int cmp")
        if (tree is IntExpression) {
            //     println("values: $value, ${tree.value}")
            if (value != tree.value) {
                //       println("Int not equal")
                return CompareResult.OtherChangeDetected
            }
            // println("Int equal")
            return CompareResult.IfNotFound
        }
        return CompareResult.OtherChangeDetected
    }

    companion object {
        fun parse(reader: Reader): IntExpression {
            val integer = reader.readInteger() ?: throw SyntaxException("Error parsing integer")
            return IntExpression(integer)
        }
    }
}

fun parseStatementList(reader: Reader, writer: Writer, scope: Scope): BaseObject? {
    var stList: BaseObject?
    try {
        stList = Statement.parse(reader, writer, scope)
    } catch (e: SyntaxException) {
        //  println("Exception1 ${e.message}")
        return null
    }
    while (true) {
        var nextStatement: Statement?
        try {
            nextStatement = Statement.parse(reader, writer, scope)
        } catch (e: SyntaxException) {
            //    println("Exception2 ${e.message}")
            return null
        }
        if (nextStatement == null) {
            return stList
        }

        stList = Cell(stList!!, nextStatement)
    }
}

fun parseTree(code: String, writer: Writer): BaseObject {
    val reader = Reader(code)
    val scope = Scope()
    val tmp = parseStatementList(reader, writer, scope)
    if (!reader.isEOF()) {
        throw RuntimeException("Some data left: ${reader.remainingCode()}")
    }

    return tmp!!
}

fun evalTree(obj: BaseObject) {
    obj.eval()
}