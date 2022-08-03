package com.github.bobi.aemgroovyconsoleplugin.console

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.impl.EditorHeaderComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.github.bobi.aemgroovyconsoleplugin.actions.AemConsoleExecuteAction
import com.github.bobi.aemgroovyconsoleplugin.actions.AemServerChooserAction
import com.github.bobi.aemgroovyconsoleplugin.config.SettingsChangedNotifier
import com.github.bobi.aemgroovyconsoleplugin.config.ui.AemServersConfigurable
import com.github.bobi.aemgroovyconsoleplugin.console.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.console.GroovyConsoleUserData.setCurrentAemServerId
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import javax.swing.JComponent

class EditorDecorator(project: Project) : EditorNotifications.Provider<JComponent>() {
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

        if (currentServer == null) {
            return EditorNotificationPanel().apply {
                text = "AEM Servers configuration is missing"
                createActionLabel("Configure") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, AemServersConfigurable::class.java)
                }
            }
        }

        return EditorHeaderComponent().also { headerComponent ->
            val actionGroup = DefaultActionGroup().also { actionGroup ->
                actionGroup.add(
                    AemConsoleExecuteAction().also {
                        it.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, fileEditor.component)
                    }
                )
                actionGroup.addSeparator()
                actionGroup.add(AemServerChooserAction(project))
            }

            val toolbar = ActionManager.getInstance().createActionToolbar("AemGroovyConsole", actionGroup, true)

            toolbar.setTargetComponent(headerComponent)
            headerComponent.add(toolbar.component)
        }
    }
}