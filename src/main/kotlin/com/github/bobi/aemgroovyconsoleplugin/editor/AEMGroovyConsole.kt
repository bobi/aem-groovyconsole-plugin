package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.removeConsole
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.intellij.execution.Executor
import com.intellij.execution.filters.RegexpFilter
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.*
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import javax.swing.JPanel

class AEMGroovyConsole(
    private val project: Project,
    private val contentFile: VirtualFile,
    private val executor: Executor
) {

    private val consoleView: ConsoleView

    private val descriptor: RunContentDescriptor

    private val scriptExecutor: AemGroovyConsoleScriptExecutor

    private val serverId: Long

    init {
        val serverConfig = contentFile.getCurrentAemConfig(project)!!

        serverId = serverConfig.id

        consoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .filters(
                RegexpFilter(
                    project,
                    "at Script1.run(${RegexpFilter.FILE_PATH_MACROS}:${RegexpFilter.LINE_MACROS}).*"
                )
            )
            .console.also {
                Disposer.register(it) { contentFile.removeConsole(serverConfig.id) }
            }

        descriptor = RunContentDescriptor(
            consoleView,
            null,
            JPanel(BorderLayout()),
            "[${serverConfig.name}] - [${contentFile.name}]"
        ).also { descr ->
            descr.executionId = serverConfig.id
            descr.reusePolicy = object : RunContentDescriptorReusePolicy() {
                override fun canBeReusedBy(newDescriptor: RunContentDescriptor): Boolean =
                    descr.executionId == newDescriptor.executionId
            }
        }

        scriptExecutor = AemGroovyConsoleScriptExecutor(project, consoleView)

        descriptor.component.also { ui ->
            val consoleViewComponent = consoleView.component

            val actionGroup = DefaultActionGroup().also { ag ->
                ag.add(object : AnAction("Restart The Script", "Run the script again", AllIcons.Actions.Restart) {
                    override fun actionPerformed(e: AnActionEvent) {
                        executeScript()
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabled = !scriptExecutor.isExecuting()
                    }
                })
                ag.addSeparator()
                ag.addAll(*consoleView.createConsoleActions())
                ag.add(CloseAction(executor, descriptor, project))
            }

            val toolbar = ActionManager.getInstance()
                .createActionToolbar("AEMGroovyConsole", actionGroup, false)
                .also { tb ->
                    tb.setTargetComponent(consoleViewComponent)
                }

            ui.add(consoleViewComponent, BorderLayout.CENTER)
            ui.add(toolbar.component, BorderLayout.WEST)
        }
    }

    fun executeScript() {
        val config = PersistentStateService.getInstance(project).findById(serverId)

        if (config == null) {
            consoleView.clear()

            consoleView.print(
                "\nAEM Config is not found for server: $serverId\n\n",
                ConsoleViewContentType.LOG_WARNING_OUTPUT
            )
        } else {
            scriptExecutor.execute(contentFile, config)
        }
    }

    fun toFrontRunContent() {
        RunContentManager.getInstance(project).toFrontRunContent(executor, descriptor)
    }

    fun showRunContent() {
        RunContentManager.getInstance(project).showRunContent(executor, descriptor)
    }
}