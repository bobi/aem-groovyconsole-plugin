package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.config.SettingsChangedNotifier
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.setCurrentAemServerId
import com.github.bobi.aemgroovyconsoleplugin.editor.actions.AemConsoleEditorToolbar
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import javax.swing.JComponent

class AemConsoleEditorDecorator(project: Project) : EditorNotifications.Provider<JComponent>() {
    companion object {
        private val myKey = Key.create<JComponent>("aem.groovy.console.toolbar")
    }

    init {
        val notifications = project.getService(EditorNotifications::class.java)

        project.messageBus.connect(project).subscribe(SettingsChangedNotifier.TOPIC, object : SettingsChangedNotifier {
            override fun settingsChanged() {
                notifications.updateAllNotifications()
            }
        })
    }

    override fun getKey(): Key<JComponent> = myKey

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): JComponent? {
        if (!file.isAemFile()) {
            return null
        }

        val configFromFile = file.getCurrentAemConfig(project)
        val availableServers = PersistentStateService.getInstance(project).getAEMServers()

        val currentServer: AemServerConfig? = configFromFile ?: availableServers.firstOrNull()

        file.setCurrentAemServerId(currentServer?.id)

        return AemConsoleEditorToolbar(project, fileEditor)
    }
}