package org.solution

import javax.swing.JTextArea
import kotlinx.coroutines.*
import java.lang.Exception

class InterpreterLauncher(val input: JTextArea, val output: JTextArea, val writer: Writer = Writer(output)) {

    var ifCount = 0
    fun buildSyntaxTree() { // todo add new if detection
        GlobalScope.launch {
            while (true) {
                try {
                    val (tree, newIfCOunt) = parseTree(input.text, writer) // todo return ifCount ?
                    if (newIfCOunt > ifCount) {
                        writer.set("New if detected")
                    }
                    ifCount = newIfCOunt
                } catch (e: Exception) {
                    // syntax parsing error
                }
                Thread.sleep(1000L)
            }
        }
    }

    fun runInterpreter() {
        try {
            val (tree, ifCount) = parseTree(input.text, writer)
            writer.set("")
            evalTree(tree)
        } catch (e: Exception) {
            System.err.println("Interpreting error")
        }
    }
}