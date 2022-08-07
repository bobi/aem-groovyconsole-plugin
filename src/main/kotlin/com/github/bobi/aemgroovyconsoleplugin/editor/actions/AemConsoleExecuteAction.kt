package com.github.bobi.aemgroovyconsoleplugin.editor.actions

import com.github.bobi.aemgroovyconsoleplugin.editor.AemGroovyConsoleService
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemServerId
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager

class AemConsoleExecuteAction : AnAction(AllIcons.Actions.Execute) {

    init {
        templatePresentation.text = "Run"
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        FileDocumentManager.getInstance().saveAllDocuments()

        val project = e.project
        val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)

        if (project != null && editor != null && virtualFile != null) {
            AemGroovyConsoleService.getInstance(project).execute(virtualFile)
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
}