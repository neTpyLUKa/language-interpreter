package org.solution

import javax.swing.JTextArea

open class Writer(val area: JTextArea? = null) {
    var sb = StringBuilder()
    open fun append(string: String) {
        sb.append(string)
        sb.append('\n')
        area!!.text = sb.toString()
    }

    open fun set(string: String) {
        sb = StringBuilder()
        append(string)
    }
}