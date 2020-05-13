package org.solution

import kotlin.text.StringBuilder

class Reader constructor(private val code: String, private val size: Int = code.length) {
    var pos = 0

    private fun skipSpaces() {
        while (pos < size && code[pos].isWhitespace()) {
            ++pos
        }
    }

    fun isEOF(): Boolean {
        skipSpaces()
        return pos == size
    }

    fun remainingCode(): String {
        return code.substring(pos, size)
    }

    fun read(string: String): Boolean {
        skipSpaces()
        val end = pos + string.length
        if (end > size) {
            return false
        }
        val sub = code.substring(pos, pos + string.length)
        if (sub == string) {
            pos = end
            return true
        }
        return false
    }

    private fun identifierReadFirst(): Char? {
        if (pos >= size) {
            return null
        }
        if (code[pos] == '_' || code[pos].isLetter()) {
            return code[pos++]
        }
        return null
    }

    private fun identifierReadRemaining(): Char? {
        if (pos >= size) {
            return null
        }
        if (code[pos] == '_' || code[pos].isLetterOrDigit()) {
            return code[pos++]
        }
        return null
    }

    fun readIdentifier(): String? {
        skipSpaces()
        var ch: Char? = identifierReadFirst() ?: return null
        val sb = StringBuilder()
        sb.append(ch)
        while (true) {
            ch = identifierReadRemaining()
            if (ch == null) {
                return sb.toString()
            }
            sb.append(ch)
        }
    }

    private fun readSign(): Char? {
        if (pos >= size) {
            return null
        }
        if (code[pos] == '+' || code[pos] == '-') {
            return code[pos++]
        }
        return null
    }

    private fun undoReadSign(sign: Char?) {
        if (sign != null) {
            --pos
        }
    }

    private fun readDigit(): Char? {
        if (pos < size && code[pos].isDigit()) {
            return code[pos++]
        }
        return null
    }

    fun readInteger(): Int? {
        skipSpaces()
        val sign = readSign()
        var ch: Char? = readDigit()
        if (ch == null) {
            undoReadSign(sign)
            return null
        }
        val sb = StringBuilder()
        sb.append(ch)
        while (true) {
            ch = readDigit()
            if (ch == null) {
                var value = sb.toString().toInt()
                if (sign == '-') {
                    value *= -1
                }
                return value
            }
            sb.append(ch)
        }
    }

    fun peek(): Char? {
        if (isEOF()) {
            return null
        }
        //spaces skipped in eof
        return code[pos]
    }
}