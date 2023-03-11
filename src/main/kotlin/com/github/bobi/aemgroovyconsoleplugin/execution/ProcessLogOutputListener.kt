package com.github.bobi.aemgroovyconsoleplugin.execution

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
    private var writer: Writer = PrintWriter(file)

    override fun processTerminated(event: ProcessEvent) {
        writer.flush()
        writer.close()
        event.processHandler.removeProcessListener(this)
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        writer.write(event.text)
    }
}