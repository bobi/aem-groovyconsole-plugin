package com.github.bobi.aemgroovyconsoleplugin.actions

import com.github.bobi.aemgroovyconsoleplugin.config.SettingsChangedNotifier
import com.github.bobi.aemgroovyconsoleplugin.services.RootFoldersService
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.FileContentUtil
import icons.JetgroovyIcons

@Suppress("DialogTitleCapitalization")
class MarkAsAemScriptsRootAction : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull()

        if (project == null
            || virtualFile == null
            || !virtualFile.isDirectory
            || e.place != ActionPlaces.PROJECT_VIEW_POPUP
        ) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val rfs = RootFoldersService.getInstance(project)

        if (rfs.hasRoot(virtualFile.path)) {
            e.presentation.text = "Unmark as AEM Scripts Source Root"
            e.presentation.description = "Unmark as AEM Scripts Source Root"
        } else {
            e.presentation.text = "AEM Scripts Source Root"
            e.presentation.description = "Mark as AEM Scripts Source Root"
            e.presentation.icon = JetgroovyIcons.Groovy.Groovy_16x16
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull() ?: return

        if (!virtualFile.isDirectory || !e.presentation.isEnabledAndVisible) {
            return
        }

        val rfs = RootFoldersService.getInstance(project)

        if (rfs.hasRoot(virtualFile.path)) {
            rfs.removeRoot(virtualFile.path)
        } else {
            rfs.addRoot(virtualFile.path)
        }

        project.messageBus.syncPublisher(SettingsChangedNotifier.TOPIC).settingsChanged()

        FileContentUtil.reparseOpenedFiles()
    }
}