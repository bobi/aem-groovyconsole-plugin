package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleTable
import com.intellij.util.containers.toArray

/**
 * User: Andrey Bardashevsky
 * Date/Time: 06.08.2022 19:11
 */
class ConsoleTableFormatter(private val table: GroovyConsoleTable) {

    private fun calculateColumnLengths(): IntArray {
        val columnLengths = IntArray(table.columns.size) { 0 }

        table.columns.forEachIndexed {i, colValue ->
            if (columnLengths[i] < colValue.length) {
                columnLengths[i] = colValue.length
            }
        }

        table.rows.forEach { cols ->
            cols.forEachIndexed { i, colValue ->
                if (columnLengths[i] < colValue.length) {
                    columnLengths[i] = colValue.length
                }
            }
        }

        return columnLengths
    }

    private fun buildFormatString(columnLengths: IntArray): String {
        return columnLengths.map { "| %-${it}s " }.reduce { v1, v2 -> v1 + v2 } + "|%n"
    }

    private fun buildHorizontalSeparator(columnLengths: IntArray): String {
        return columnLengths.map { "+-${"-".repeat(it)}-" }.reduce { v1, v2 -> v1 + v2 } + String.format("+%n")
    }

    fun print(lineOutput: (line: String) -> Unit) {
        val columnLengths = calculateColumnLengths()
        val formatString = buildFormatString(columnLengths)
        val horizontalSeparator = buildHorizontalSeparator(columnLengths)

        lineOutput(horizontalSeparator)
        lineOutput(String.format(formatString, *table.columns.toArray(emptyArray())))
        lineOutput(horizontalSeparator)
        table.rows.forEach { lineOutput(String.format(formatString, *it.toArray(emptyArray()))) }
        lineOutput(horizontalSeparator)
    }
}