package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.utils.Notifications
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
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.JPanel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 07.08.2022 18:33
 */
class AemGroovyConsoleScriptExecutor(private val project: Project) {

    private val tmpFolder: File by lazy {
        FileUtil.createTempDirectory("aem-groovy-console", null, true)
    }

    fun execute(contentFile: VirtualFile) {
        val config = contentFile.getCurrentAemConfig(project) ?: return

        val oldDescriptor = findDescriptor(project, contentFile, config.id)

        doExecute(oldDescriptor, contentFile, config.id)
    }

    private fun doExecute(
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

                val newDescriptor =
                    AemConsoleRunContentDescriptor(project, contentFile, config, console, consolePanel, tmpFolder)

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

        fun getInstance(project: Project): AemGroovyConsoleScriptExecutor = project.service()

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
                    tbActions.add(SaveOutputAction())
                    tbActions.add(CloseAction())
                }

                val toolbar = ActionManager.getInstance()
                    .createActionToolbar("AemGroovyConsoleExecutorToolbar", toolbarActions, false).also {
                        it.targetComponent = consoleComponent
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
                    getInstance(project).doExecute(descriptor, descriptor.contentFile, descriptor.config.id)
                }
            }
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
        }
    }

    @Suppress("DialogTitleCapitalization")
    private class SaveOutputAction : AnAction({ "Save output" }, Presentation.NULL_STRING, AllIcons.Actions.Download),
        DumbAware {

        override fun update(e: AnActionEvent) {
            val project = e.project
            val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

            if (project != null && component != null) {
                val descriptor = findDescriptor(project, component)

                e.presentation.isEnabled =
                    (descriptor != null
                            && descriptor.processHandler != null
                            && descriptor.processHandler!!.isProcessTerminated
                            && descriptor.tmpFile.exists())
            }
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project
            val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

            if (project != null && component != null) {

                val aemConsoleRunContentDescriptor = findDescriptor(project, component)

                if (aemConsoleRunContentDescriptor != null && aemConsoleRunContentDescriptor.processHandler != null
                    && aemConsoleRunContentDescriptor.processHandler!!.isProcessTerminated
                    && aemConsoleRunContentDescriptor.tmpFile.exists()
                ) {
                    var outputDir = project.guessProjectDir()
                    if (outputDir == null || !outputDir.exists()) {
                        outputDir = VfsUtil.getUserHomeDir()
                    }

                    val extension = "txt"
                    val saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(
                        FileSaverDescriptor("Save AEM Groovy Console output", "", extension),
                        project
                    )

                    val fileName =
                        "groovy-console-%1\$tF-%1\$tH%1\$tM%1\$tS"
                            .let { if (SystemInfo.isMac) "$it.$extension" else it }
                            .let { String.format(Locale.US, it, Calendar.getInstance()) }

                    val fileWrapper = saveFileDialog.save(outputDir, fileName)

                    fileWrapper?.let {
                        try {
                            aemConsoleRunContentDescriptor.tmpFile.copyTo(it.file, true)
                            
                            Notifications.notifyInfo("Output saved", it.file.canonicalPath)
                        } catch (e: IOException) {
                            Notifications.notifyError("Save Output Error", e.localizedMessage)
                        }
                    }
                }
            }
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.EDT
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