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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import java.util.function.Function
import javax.swing.JComponent

class AemConsoleEditorDecorator(project: Project) : EditorNotificationProvider {

    init {
        val notifications = project.getService(EditorNotifications::class.java)

        project.messageBus.connect(project).subscribe(SettingsChangedNotifier.TOPIC, object : SettingsChangedNotifier {
            override fun settingsChanged() {
                notifications.updateAllNotifications()
            }
        })
    }

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile
    ): Function<in FileEditor, out JComponent?> {
        if (!file.isAemFile(project)) {
            return EditorNotificationProvider.CONST_NULL
        }

        val configFromFile = file.getCurrentAemConfig(project)

        val currentServer: AemServerConfig? = configFromFile
            ?: PersistentStateService.getInstance(project).getAEMServers().firstOrNull()

        file.setCurrentAemServerId(currentServer?.id)

        return Function<FileEditor, JComponent?> { AemConsoleEditorToolbar(project, it) }
    }
}