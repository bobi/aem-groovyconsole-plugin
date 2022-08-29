package com.github.bobi.aemgroovyconsoleplugin.editor.actions

import com.github.bobi.aemgroovyconsoleplugin.config.ui.AemServersConfigurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.editor.impl.EditorHeaderComponent
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import java.awt.BorderLayout

/**
 * User: Andrey Bardashevsky
 * Date/Time: 04.08.2022 15:03
 */
class AemConsoleEditorToolbar(project: Project, fileEditor: FileEditor) : EditorHeaderComponent() {
    init {
        val leftActionGroup = DefaultActionGroup().also { ag ->
            ag.add(
                AemConsoleExecuteAction().also {
                    it.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, fileEditor.component)
                }
            )
            ag.addSeparator()
            ag.add(AemConsoleServerChooserAction(project))
        }

        val rightActionGroup = DefaultActionGroup().also { ag ->
            ag.add(object: AnAction("Open Configuration Dialog", "", AllIcons.General.Settings) {
                override fun actionPerformed(e: AnActionEvent) {
                    val proj = e.project

                    if (proj != null) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(proj, AemServersConfigurable::class.java)
                    }
                }
            })
        }


        val actionManager = ActionManager.getInstance()

        val leftToolbar = actionManager.createActionToolbar("LeftAemGroovyConsoleActionGroup", leftActionGroup, true).also {
            it.targetComponent = this
        }

        val rightToolbar = actionManager.createActionToolbar("RightAemGroovyConsoleActionGroup", rightActionGroup, true).also {
            it.targetComponent = this
        }

        this.add(leftToolbar.component, BorderLayout.WEST)

        this.add(rightToolbar.component, BorderLayout.EAST)
    }
}