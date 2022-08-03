package com.github.bobi.aemgroovyconsoleplugin.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.github.bobi.aemgroovyconsoleplugin.console.AEMGroovyConsole

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
            AEMGroovyConsole.getOrCreateConsole(project, virtualFile) {
                it.execute()
            }
        }
    }
}