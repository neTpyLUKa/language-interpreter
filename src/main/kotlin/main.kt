package org.solution

import javax.swing.JTextArea
import kotlinx.coroutines.*
import java.lang.Exception

class InterpreterLauncher(
    private val input: JTextArea,
    private val output: JTextArea,
    private val writer: Writer = Writer(output)
) {

    private var prev: BaseObject? = null

    fun buildSyntaxTree() {
        GlobalScope.launch {
            while (true) {
                try {
                    val tree = parseTree(input.text, writer)
                    if (prev != null) {
                        val res = tree.compareWithPrev(prev!!)
                        //   println(res)
                        if (res == CompareResult.NewIfFound) {
                            writer.set("New if detected")
                        }
                    }
                    prev = tree
                } catch (e: Exception) {
                    // syntax parsing error
                }
                //     Thread.sleep(1000L)
            }
        }
    }

    fun runInterpreter() {
        writer.set("")
        try {
            val tree = parseTree(input.text, writer)
            evalTree(tree)
        } catch (e: Exception) {
            System.err.println("Interpreting error")
            writer.set("$e\n${e.message}")
        }
    }
}