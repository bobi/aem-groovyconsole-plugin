package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.RegexpFilter
import com.intellij.execution.filters.RegexpFilter.FILE_PATH_MACROS
import com.intellij.execution.filters.RegexpFilter.LINE_MACROS
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.*
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel

class AEMGroovyConsole(
    private val project: Project,
    private val descriptor: RunContentDescriptor,
    private val view: ConsoleView,
    private val contentFile: VirtualFile,
    private val serverId: Long
) {
    private val httpService = GroovyConsoleHttpService.getInstance(project)
    private val executing = AtomicBoolean(false)

    fun execute() {
        if (executing.compareAndSet(false, true)) {
            view.clear()
            view.scrollTo(0)

            val config = PersistentStateService.getInstance(project).findById(serverId)

            if (config == null) {
                view.print(
                    "\nAEM Config is not found for server: $serverId\n\n",
                    ConsoleViewContentType.LOG_WARNING_OUTPUT
                )
                return
            }

            view.print("\nRunning script on ${config.url}\n\n", ConsoleViewContentType.NORMAL_OUTPUT)

            RunContentManager.getInstance(project).toFrontRunContent(defaultExecutor, descriptor)

            runBackgroundableTask("Running AEM Script ${contentFile.name} on ${config.url}", project, false) {
                doExecute(config) {
                    executing.compareAndSet(true, false)
                }
            }
        }
    }

    private fun doExecute(config: AemServerConfig, completeCallback: () -> Unit) {
        try {
            val output = httpService.execute(config, contentFile.contentsToByteArray())

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
                    view.print(
                        output.exceptionStackTrace.replace(
                            "Script1.groovy",
                            contentFile.name
                        ),
                        ConsoleViewContentType.ERROR_OUTPUT
                    )
                }

                view.print("\nExecution Time:${output.runningTime}", ConsoleViewContentType.LOG_WARNING_OUTPUT)

                completeCallback()
            }
        } catch (th: Throwable) {
            runInEdt {
                view.print(th.localizedMessage, ConsoleViewContentType.ERROR_OUTPUT)

                completeCallback()
            }
        }
    }

    companion object {
        private val GROOVY_CONSOLES = Key.create<MutableMap<Long, AEMGroovyConsole>>("AEMGroovyConsoles")

        private val defaultExecutor = DefaultRunExecutor.getRunExecutorInstance()

        fun getOrCreateConsole(
            project: Project,
            contentFile: VirtualFile,
            callback: (console: AEMGroovyConsole) -> Unit
        ) {
            val currentServerConfig = contentFile.getCurrentAemConfig(project) ?: return

            val existingConsole = contentFile.getConsole(currentServerConfig.id)

            if (existingConsole != null) {
                callback(existingConsole)
            } else {
                val console = createConsole(project, contentFile, currentServerConfig)

                contentFile.addConsole(currentServerConfig.id, console)

                callback(console)
            }
        }

        private fun createConsole(
            project: Project,
            contentFile: VirtualFile,
            server: AemServerConfig
        ): AEMGroovyConsole {
            val consoleView = TextConsoleBuilderFactory.getInstance()
                .createBuilder(project)
                .filters(RegexpFilter(project, "at Script1.run($FILE_PATH_MACROS:$LINE_MACROS).*"))
                .console.also {
                    Disposer.register(it) { contentFile.removeConsole(server.id) }
                }

            val descriptor = RunContentDescriptor(
                consoleView,
                null,
                JPanel(BorderLayout()),
                "[${server.name}] - [${contentFile.name}]"
            ).also { descr ->
                descr.executionId = server.id
                descr.reusePolicy = object : RunContentDescriptorReusePolicy() {
                    override fun canBeReusedBy(newDescriptor: RunContentDescriptor): Boolean =
                        descr.executionId == newDescriptor.executionId
                }
            }

            val console = AEMGroovyConsole(project, descriptor, consoleView, contentFile, server.id)

            descriptor.component.also { ui ->
                val consoleViewComponent = consoleView.component

                val actionGroup = DefaultActionGroup().also { ag ->
                    ag.add(object : AnAction("Restart The Script", "Run the script again", AllIcons.Actions.Restart) {
                        override fun actionPerformed(e: AnActionEvent) {
                            console.execute()
                        }
                    })
                    ag.addSeparator()
                    ag.addAll(*consoleView.createConsoleActions())
                    ag.add(CloseAction(defaultExecutor, descriptor, project))
                }

                val toolbar = ActionManager.getInstance()
                    .createActionToolbar("AEMGroovyConsole", actionGroup, false)
                    .also { tb ->
                        tb.setTargetComponent(consoleViewComponent)
                    }

                ui.add(consoleViewComponent, BorderLayout.CENTER)
                ui.add(toolbar.component, BorderLayout.WEST)
            }

            RunContentManager.getInstance(project).showRunContent(defaultExecutor, descriptor)

            return console
        }

        private fun VirtualFile.getConsole(serverId: Long): AEMGroovyConsole? {
            return getUserData(GROOVY_CONSOLES)?.get(serverId)
        }

        private fun VirtualFile.addConsole(serverId: Long, console: AEMGroovyConsole) {
            putUserDataIfAbsent(GROOVY_CONSOLES, mutableMapOf())[serverId] = console
        }

        private fun VirtualFile.removeConsole(serverId: Long): AEMGroovyConsole? {
            return putUserDataIfAbsent(GROOVY_CONSOLES, mutableMapOf()).remove(serverId)
        }
    }
}