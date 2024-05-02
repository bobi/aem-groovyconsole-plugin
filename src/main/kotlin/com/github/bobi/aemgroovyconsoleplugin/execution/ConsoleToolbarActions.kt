package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.utils.Notifications
import com.github.bobi.aemgroovyconsoleplugin.utils.openSaveFileDialog
import com.intellij.CommonBundle
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAware
import java.awt.Component
import java.io.IOException

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-03 19:11
 */
internal class RerunAction(private val component: Component) : AnAction(
    CommonBundle.message("action.text.rerun"), CommonBundle.message("action.text.rerun"), AllIcons.Actions.Restart
), DumbAware {
    override fun update(e: AnActionEvent) {
        val project = e.project

        if (project != null) {
            val descriptor = AemGroovyConsoleScriptExecutor.findDescriptor(project, component)

            e.presentation.isEnabled =
                (descriptor == null || descriptor.processHandler == null || descriptor.processHandler!!.isProcessTerminated)
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (project != null) {
            val descriptor = AemGroovyConsoleScriptExecutor.findDescriptor(project, component)

            if (descriptor != null) {
                AemGroovyConsoleScriptExecutor.getInstance(project).doExecute(
                    descriptor, descriptor.contentFile, descriptor.config.id, descriptor.action
                )
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

internal class SaveOutputAction : AnAction({ "Save output" }, Presentation.NULL_STRING, AllIcons.Actions.Download),
    DumbAware {

    override fun update(e: AnActionEvent) {
        val project = e.project
        val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

        if (project != null && component != null) {
            val descriptor = AemGroovyConsoleScriptExecutor.findDescriptor(project, component)

            e.presentation.isEnabled =
                (descriptor != null && descriptor.processHandler != null && descriptor.processHandler!!.isProcessTerminated && descriptor.tmpFile.exists())
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)

        if (project != null && component != null) {

            val aemConsoleRunContentDescriptor = AemGroovyConsoleScriptExecutor.findDescriptor(project, component)

            if (aemConsoleRunContentDescriptor != null && aemConsoleRunContentDescriptor.processHandler != null && aemConsoleRunContentDescriptor.processHandler!!.isProcessTerminated && aemConsoleRunContentDescriptor.tmpFile.exists()) {
                val fileWrapper = openSaveFileDialog(project, "txt", "Save AEM Groovy Console output")

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

internal class CloseAction(private val component: Component) : AnAction(
    ExecutionBundle.messagePointer("close.tab.action.name"), Presentation.NULL_STRING, AllIcons.Actions.Cancel
), DumbAware {
    init {
        ActionUtil.copyFrom(this, IdeActions.ACTION_CLOSE)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (project != null) {
            val descriptor: RunContentDescriptor? = AemGroovyConsoleScriptExecutor.findDescriptor(project, component)

            if (descriptor != null) {
                RunContentManager.getInstance(project).removeRunContent(AemGroovyConsoleScriptExecutor.executor, descriptor)
            }
        }
    }
}
