package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang3.exception.ExceptionUtils
import org.jetbrains.concurrency.AsyncPromise
import java.io.OutputStream

/**
 * User: Andrey Bardashevsky
 * Date/Time: 18.08.2022 16:29
 */
class AemGroovyConsoleProcessHandler(
    private val project: Project,
    private val contentFile: VirtualFile,
    private val config: AemServerConfig,
    private val action: GroovyConsoleHttpService.Action,
) : ProcessHandler() {

    private val httpService = GroovyConsoleHttpService.getInstance(project)

    init {
        addProcessListener(object : ProcessAdapter() {
            override fun startNotified(event: ProcessEvent) {
                execute()
            }
        })
    }

    private fun execute() {
        print("\nRunning AEM Script [${contentFile.name}] on ${config.url}\n\n")

        val promise = AsyncPromise<GroovyConsoleOutput>().also {
            it.onSuccess { out ->
                runInEdt {
                    handleOutput(out)
                    notifyProcessTerminated(0)
                }
            }.onError { ex ->
                runInEdt {
                    val rootCause = ExceptionUtils.getRootCause(ex)
                    val message = rootCause.localizedMessage ?: rootCause.javaClass.simpleName
                    printError(message)
                    notifyProcessTerminated(1)
                }
            }
        }

        runBackgroundableTask("Running AEM Script ${contentFile.name} on ${config.url}", project, false) {
            try {
                promise.setResult(httpService.execute(config, contentFile.contentsToByteArray(), action))
            } catch (th: Throwable) {
                thisLogger().info(th)
                promise.setError(th)
            }
        }
    }

    private fun handleOutput(output: GroovyConsoleOutput) {
        if (output.exceptionStackTrace.isNullOrBlank()) {
            if (!output.output.isNullOrBlank()) {
                print(output.output)
            }

            if (output.table != null && !output.result.isNullOrBlank()) {
                notifyTextAvailable(output.result, AemConsoleOutputType.TABLE_OUTPUT_TYPE)
            } else if (!output.result.isNullOrBlank()) {
                if (!output.output.isNullOrBlank()) {
                    print("\n")
                }
                print(output.result)
                print("\n")
            }
        } else {
            //This code relies on fact that AEM Groovy Console uses Script1.groovy as file name, so this code is highly dangerous
            //In some obvious cases it could work incorrectly, but it provides user with better experience
            printError(output.exceptionStackTrace.replace("Script1.groovy", contentFile.name))
        }

        if (!output.runningTime.isNullOrBlank()) {
            print("\nExecution Time: ${output.runningTime}")
        }
    }

    private fun print(text: String) = notifyTextAvailable(text, ProcessOutputType.STDOUT)

    private fun printError(text: String) = notifyTextAvailable(text, ProcessOutputType.STDERR)

    override fun destroyProcessImpl() = Unit

    override fun detachProcessImpl() = Unit

    override fun detachIsDefault(): Boolean = false

    override fun getProcessInput(): OutputStream? = null
}
