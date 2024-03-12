package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Component
import java.io.File

/**
 * User: Andrey Bardashevsky
 * Date/Time: 07.08.2022 18:33
 */
@Service(Service.Level.PROJECT)
class AemGroovyConsoleScriptExecutor(private val project: Project) {

    private val tmpFolder: File by lazy {
        FileUtil.createTempDirectory("aem-groovy-console", null, true)
    }

    fun execute(contentFile: VirtualFile, action: GroovyConsoleHttpService.Action) {
        val config = contentFile.getCurrentAemConfig(project) ?: return

        val oldDescriptor = findDescriptor(project, contentFile, config.id)

        doExecute(oldDescriptor, contentFile, config.id, action)
    }

    internal fun doExecute(
        oldDescriptor: AemConsoleRunContentDescriptor?,
        contentFile: VirtualFile,
        serverId: Long,
        action: GroovyConsoleHttpService.Action
    ) {
        if (oldDescriptor == null || oldDescriptor.processHandler!!.isProcessTerminated) {
            oldDescriptor?.console?.clear()
            oldDescriptor?.console?.scrollTo(0)

            val config = PersistentStateService.getInstance(project).findById(serverId)

            if (config == null) {
                oldDescriptor?.console?.print(
                    "\nAEM Server is not configured\n\n",
                    ConsoleViewContentType.ERROR_OUTPUT
                )
            } else {
                documentManager.getDocument(contentFile)?.let {
                    documentManager.saveDocument(it)
                }

                val aemConsoleResultPanel =
                    oldDescriptor?.resultPanel ?: AemConsoleResultPanel(project, createConsole(project))

                val newDescriptor = AemConsoleRunContentDescriptor(
                    aemConsoleResultPanel, contentFile, config, action, tmpFolder
                )

                RunContentManager.getInstance(project).showRunContent(executor, newDescriptor, oldDescriptor)

                newDescriptor.processHandler!!.startNotify()
            }
        } else {
            RunContentManager.getInstance(project).toFrontRunContent(executor, oldDescriptor)
        }
    }


    companion object {
        internal val executor = DefaultRunExecutor.getRunExecutorInstance()

        private val documentManager: FileDocumentManager by lazy {
            return@lazy FileDocumentManager.getInstance()
        }

        fun getInstance(project: Project): AemGroovyConsoleScriptExecutor = project.service()

        private fun getAllDescriptors(project: Project) =
            project.serviceIfCreated<RunContentManager>()?.allDescriptors ?: emptyList()

        private fun createConsole(project: Project): ConsoleView {
            return TextConsoleBuilderFactory.getInstance().createBuilder(project).apply { setViewer(true) }.console
        }

        private fun findDescriptor(
            project: Project, contentFile: VirtualFile, serverId: Long
        ): AemConsoleRunContentDescriptor? {
            return getAllDescriptors(project).let { descriptors ->
                return@let descriptors.find {
                    it is AemConsoleRunContentDescriptor && it.contentFile == contentFile && it.config.id == serverId
                } as AemConsoleRunContentDescriptor?
            }
        }

        internal fun findDescriptor(project: Project, component: Component): AemConsoleRunContentDescriptor? {
            return getAllDescriptors(project).let { descriptors ->
                return@let descriptors.find {
                    it is AemConsoleRunContentDescriptor && it.console == component
                } as AemConsoleRunContentDescriptor?
            }
        }
    }
}
