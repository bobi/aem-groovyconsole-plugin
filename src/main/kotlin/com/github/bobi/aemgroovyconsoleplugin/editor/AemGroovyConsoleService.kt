package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.execution.AemConsoleRunContentDescriptor
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.intellij.CommonBundle
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 07.08.2022 18:33
 */
class AemGroovyConsoleService(private val project: Project) {

    fun execute(contentFile: VirtualFile) {
        val config = contentFile.getCurrentAemConfig(project) ?: return

        val oldDescriptor = findDescriptor(project, contentFile, config.id)

        executeScript(oldDescriptor, contentFile, config.id)
    }

    private fun executeScript(
        oldDescriptor: AemConsoleRunContentDescriptor?,
        contentFile: VirtualFile,
        serverId: Long
    ) {
        if (oldDescriptor == null || oldDescriptor.processHandler!!.isProcessTerminated) {
            oldDescriptor?.console?.clear()
            oldDescriptor?.console?.scrollTo(0)

            val config = PersistentStateService.getInstance(project).findById(serverId)

            if (config == null) {
                oldDescriptor?.console?.print("\nAEM Server is not configured\n\n", ConsoleViewContentType.ERROR_OUTPUT)
            } else {
                documentManager.getDocument(contentFile)?.let {
                    documentManager.saveDocument(it)
                }

                val console = oldDescriptor?.console ?: createConsole(project)
                val consolePanel = oldDescriptor?.component ?: createConsolePanel(console)

                val newDescriptor = AemConsoleRunContentDescriptor(project, contentFile, config, console, consolePanel)

                RunContentManager.getInstance(project).showRunContent(executor, newDescriptor, oldDescriptor)

                newDescriptor.processHandler!!.startNotify()
            }
        } else {
            RunContentManager.getInstance(project).toFrontRunContent(executor, oldDescriptor)
        }
    }

    companion object {
        private val executor = DefaultRunExecutor.getRunExecutorInstance()

        private val documentManager = FileDocumentManager.getInstance()

        fun getInstance(project: Project): AemGroovyConsoleService = project.service()

        private fun getAllDescriptors(project: Project) =
            project.serviceIfCreated<RunContentManager>()?.allDescriptors ?: emptyList()

        private fun createConsole(project: Project): ConsoleView {
            return TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        }

        private fun createConsolePanel(console: ConsoleView): JPanel {
            return JPanel(BorderLayout()).also { panel ->
                val consoleComponent = console.component

                val toolbarActions = DefaultActionGroup().also { tbActions ->
                    tbActions.add(RerunAction())
                    tbActions.addAll(*console.createConsoleActions())
                    tbActions.add(CloseAction())
                }

                val toolbar = ActionManager.getInstance()
                    .createActionToolbar("AemGroovyConsoleExecutorToolbar", toolbarActions, false).also {
                        it.setTargetComponent(consoleComponent)
                    }

                panel.add(consoleComponent, BorderLayout.CENTER)
                panel.add(toolbar.component, BorderLayout.WEST)
            }
        }

        private fun findDescriptor(
            project: Project,
            contentFile: VirtualFile,
            serverId: Long
        ): AemConsoleRunContentDescriptor? {
            return getAllDescriptors(project).let { descriptors ->
                return@let descriptors.find {
                    it is AemConsoleRunContentDescriptor && it.contentFile == contentFile && it.config.id == serverId
                } as AemConsoleRunContentDescriptor?
            }
        }

        private fun findDescriptor(project: Project, component: Component): AemConsoleRunContentDescriptor? {
            return getAllDescriptors(project).let { descriptors ->
                return@let descriptors.find {
                    it is AemConsoleRunContentDescriptor && it.console == component
                } as AemConsoleRunContentDescriptor?
            }
        }
    }

    private class RerunAction : AnAction(
        CommonBundle.message("action.text.rerun"), CommonBundle.message("action.text.rerun"), AllIcons.Actions.Restart
    ), DumbAware {
        override fun update(e: AnActionEvent) {
            val project = e.project
            val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

            if (project != null && component != null) {
                val descriptor = findDescriptor(project, component)

                e.presentation.isEnabled =
                    (descriptor == null || descriptor.processHandler == null || descriptor.processHandler!!.isProcessTerminated)
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

            if (project != null && component != null) {
                val descriptor = findDescriptor(project, component)

                if (descriptor != null) {
                    getInstance(project).executeScript(descriptor, descriptor.contentFile, descriptor.config.id)
                }
            }
        }
    }

    private class CloseAction : AnAction(
        ExecutionBundle.messagePointer("close.tab.action.name"), Presentation.NULL_STRING, AllIcons.Actions.Cancel
    ), DumbAware {
        init {
            ActionUtil.copyFrom(this, IdeActions.ACTION_CLOSE)
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

            if (project != null && component != null) {
                val descriptor: RunContentDescriptor? = findDescriptor(project, component)

                if (descriptor != null) {
                    RunContentManager.getInstance(project).removeRunContent(executor, descriptor)
                }
            }
        }
    }

}