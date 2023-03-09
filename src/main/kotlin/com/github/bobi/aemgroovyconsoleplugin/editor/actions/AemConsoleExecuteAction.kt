package com.github.bobi.aemgroovyconsoleplugin.editor.actions

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemServerId
import com.github.bobi.aemgroovyconsoleplugin.execution.AemGroovyConsoleScriptExecutor
import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware

class AemConsoleExecuteAction : AnAction({ "Run" }, AllIcons.Actions.Execute), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)

        if (component is ActionButton || ActionPlaces.isShortcutPlace(e.place)) {
            FileDocumentManager.getInstance().saveAllDocuments()

            val project = e.project
            val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
            val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

            if (project != null && editor != null && virtualFile != null) {
                AemGroovyConsoleScriptExecutor.getInstance(project)
                    .execute(virtualFile, GroovyConsoleHttpService.Action.EXECUTE)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

        if (virtualFile != null) {
            val serverId = virtualFile.getCurrentAemServerId()

            e.presentation.isEnabled = serverId != null
        } else {
            e.presentation.isEnabled = false
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

class AemConsoleExecuteDistributedAction : AnAction({ "Run Distributed" }, AllIcons.Actions.RunAll), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)

        if (component is ActionButton) {
            FileDocumentManager.getInstance().saveAllDocuments()

            val project = e.project
            val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
            val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

            if (project != null && editor != null && virtualFile != null) {
                val config = virtualFile.getCurrentAemConfig(project)

                if (config != null && config.distributedExecution) {
                    AemGroovyConsoleScriptExecutor.getInstance(project)
                        .execute(virtualFile, GroovyConsoleHttpService.Action.EXECUTE_DISTRIBUTED)
                }
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

        if (project != null && virtualFile != null) {
            val config = virtualFile.getCurrentAemConfig(project)

            e.presentation.isEnabled = config != null && config.distributedExecution
        } else {
            e.presentation.isEnabled = false
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}