package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * User: Andrey Bardashevsky
 * Date/Time: 07.08.2022 16:42
 */
class AemGroovyConsoleScriptExecutor(
    private val project: Project,
    private val view: ConsoleView,
) {
    private val httpService = GroovyConsoleHttpService.getInstance(project)

    private val executing = AtomicBoolean(false)

    fun execute(contentFile: VirtualFile, config: AemServerConfig) {
        if (executing.compareAndSet(false, true)) {
            view.clear()
            view.scrollTo(0)

            view.print("\nRunning script on ${config.url}\n\n", ConsoleViewContentType.NORMAL_OUTPUT)

            doExecute(contentFile, config).whenComplete { output, error ->
                if (error != null) {
                    printError(error.localizedMessage)
                } else if (output != null) {
                    printOutput(contentFile, output)
                }

                executing.compareAndSet(true, false)
            }
        }
    }

    private fun printOutput(contentFile: VirtualFile, output: GroovyConsoleOutput) {
        runInEdt {
            if (output.exceptionStackTrace.isBlank()) {
                view.print(output.output, ConsoleViewContentType.NORMAL_OUTPUT)

                if (output.table != null) {
                    ConsoleTableFormatter(output.table).print {
                        view.print(it, ConsoleViewContentType.NORMAL_OUTPUT)
                    }
                }
            } else {
                //This code relies on fact that AEM Groovy Console uses Script1.groovy as file name, so this code is highly dangerous
                //In some obvious cases it could work incorrectly, but it provides user with better experience
                printError(
                    output.exceptionStackTrace.replace(
                        "Script1.groovy",
                        contentFile.name
                    )
                )
            }

            view.print("\nExecution Time:${output.runningTime}", ConsoleViewContentType.LOG_WARNING_OUTPUT)
        }
    }

    private fun printError(error: String) {
        runInEdt {
            view.print(error, ConsoleViewContentType.ERROR_OUTPUT)
        }
    }

    private fun doExecute(contentFile: VirtualFile, config: AemServerConfig): CompletableFuture<GroovyConsoleOutput> {
        val supplier = CompletableFuture<GroovyConsoleOutput>()

        runBackgroundableTask("Running AEM Script ${contentFile.name} on ${config.url}", project, false) {
            try {
                val output = httpService.execute(config, contentFile.contentsToByteArray())

                supplier.complete(output)
            } catch (th: Throwable) {
                thisLogger().info(th)

                supplier.completeExceptionally(th)
            }
        }

        return supplier
    }

    fun isExecuting(): Boolean = executing.get()
}