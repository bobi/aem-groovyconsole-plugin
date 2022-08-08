package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleTable
import com.intellij.util.containers.toArray

/**
 * User: Andrey Bardashevsky
 * Date/Time: 06.08.2022 19:11
 */
class ConsoleTableFormatter(private val table: GroovyConsoleTable) {

    private val columnLengths = IntArray(table.columns.size) { 0 }
        .also { arr ->
            table.columns.forEachIndexed { i, colValue ->
                if (arr[i] < colValue.length) {
                    arr[i] = colValue.length
                }
            }

            table.rows.forEach { cols ->
                cols.forEachIndexed { i, colValue ->
                    if (arr[i] < colValue.length) {
                        arr[i] = colValue.length
                    }
                }
            }
        }

    private val formatString = columnLengths.joinToString(
        separator = " | ",
        prefix = "| ",
        postfix = " |%n"
    ) { "%-${it}s" }

    private val horizontalSeparator = columnLengths.joinToString(
        separator = "-+-",
        prefix = "+-",
        postfix = String.format("-+%n")
    ) { "-".repeat(it) }

    fun print(lineConsumer: (line: String) -> Unit) {
        lineConsumer(horizontalSeparator)
        lineConsumer(String.format(formatString, *table.columns.toArray(emptyArray())))
        lineConsumer(horizontalSeparator)

        if (table.rows.isNotEmpty()) {
            table.rows.forEach { lineConsumer(String.format(formatString, *it.toArray(emptyArray()))) }

            lineConsumer(horizontalSeparator)
        }
    }
}