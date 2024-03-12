package com.github.bobi.aemgroovyconsoleplugin.execution

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.util.Key

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-03 19:19
 */
class AemConsoleOutputType {
    companion object {
        val TABLE_OUTPUT_TYPE = Key.create<Any>("com.github.bobi.aemgroovyconsoleplugin.execution.table.output")
        val TABLE_OUTPUT = ConsoleViewContentType("table.output", ConsoleViewContentType.NORMAL_OUTPUT_KEY)

        init {
            ConsoleViewContentType.registerNewConsoleViewType(
                TABLE_OUTPUT_TYPE, TABLE_OUTPUT
            )
        }
    }
}
