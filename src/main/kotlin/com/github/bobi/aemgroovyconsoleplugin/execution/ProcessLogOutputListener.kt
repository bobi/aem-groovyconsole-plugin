package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.execution.table.AemConsoleTextTableFormatter
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.OutputTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.util.Key
import java.io.File
import java.io.PrintWriter
import java.io.Writer

/**
 * User: Andrey Bardashevsky
 * Date/Time: 08.03.2023 01:25
 */
class ProcessLogOutputListener(val file: File) : ProcessListener {
    private val writer: Writer = PrintWriter(file)

    private val gson: Gson = GsonBuilder().create()

    override fun processTerminated(event: ProcessEvent) {
        writer.flush()
        writer.close()
        event.processHandler.removeProcessListener(this)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        if (outputType == AemConsoleOutputType.TABLE_OUTPUT_TYPE) {
            val table = gson.fromJson(event.text, OutputTable::class.java)

            AemConsoleTextTableFormatter(table.table).print {
                writer.write(it)
            }
        } else {
            writer.write(event.text)
        }
    }
}
