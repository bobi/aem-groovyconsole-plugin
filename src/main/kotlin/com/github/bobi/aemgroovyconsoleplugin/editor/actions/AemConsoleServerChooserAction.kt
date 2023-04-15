package com.github.bobi.aemgroovyconsoleplugin.editor.actions

import com.github.bobi.aemgroovyconsoleplugin.config.ui.AemServersConfigurable
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ComboBoxAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Fonts
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AemConsoleServerChooserAction(
    private val project: Project,
) : ComboBoxAction() {

    private val persistentStateService by lazy {
        PersistentStateService.getInstance(project)
    }

    init {
        isSmallVariant = true
        templatePresentation.icon = AllIcons.Webreferences.Server
    }

    override fun actionPerformed(e: AnActionEvent) {
        if (e.presentation.getClientProperty(ADD_CONFIGURATION_MODE) == true) {
            openConfigurationDialog(e.project)
        } else {
            super.actionPerformed(e)
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = CommonDataKeys.EDITOR.getData(e.dataContext)
        val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
        val presentation = e.presentation

        presentation.putClientProperty(ADD_CONFIGURATION_MODE, null)
        presentation.description = ""

        if (project != null && editor != null && virtualFile != null) {
            val config = virtualFile.getCurrentAemConfig(project)

            if (config == null) {
                if (PersistentStateService.getInstance(project).getAEMServers().isEmpty()) {
                    presentation.putClientProperty(ADD_CONFIGURATION_MODE, true)
                    presentation.text = "Add Configuration..."
                    presentation.description = "Open configuration dialog"
                } else {
                    presentation.text = "Select Configuration..."
                }
            } else {
                presentation.text = config.name
            }
        }
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return JPanel(BorderLayout(JBUI.scale(3), 0)).also { panel ->
            val comboBoxButton = createComboBoxButton(presentation)

            val label = JLabel("Run On:").also {
                it.font = Fonts.toolbarSmallComboBoxFont()
                it.labelFor = comboBoxButton
                it.border = JBUI.Borders.emptyRight(3)
            }

            panel.add(comboBoxButton, BorderLayout.CENTER)
            panel.add(label, BorderLayout.WEST)
            panel.border = JBUI.Borders.empty(0, 6, 0, 3)
        }
    }

    override fun createComboBoxButton(presentation: Presentation): ComboBoxButton {
        return object : ComboBoxButton(presentation) {
            override fun fireActionPerformed(e: ActionEvent) {
                if (presentation.getClientProperty(ADD_CONFIGURATION_MODE) == true) {
                    val context = DataManager.getInstance().getDataContext(this)
                    val project = CommonDataKeys.PROJECT.getData(context)

                    openConfigurationDialog(project)
                } else {
                    super.fireActionPerformed(e)
                }
            }

            override fun isArrowVisible(presentation: Presentation): Boolean {
                return presentation.getClientProperty(ADD_CONFIGURATION_MODE) != true
            }
        }
    }

    override fun createPopupActionGroup(button: JComponent, dataContext: DataContext): DefaultActionGroup {
        return DefaultActionGroup(
            persistentStateService.getAEMServers().map { AemConsoleSelectServerAction(it) }
        )
    }

    private fun openConfigurationDialog(project: Project?) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AemServersConfigurable::class.java)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    companion object {
        val ADD_CONFIGURATION_MODE = Key.create<Boolean>("AddConfigurationMode")
    }
}
