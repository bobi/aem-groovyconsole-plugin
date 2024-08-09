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

            val runActions = DefaultActionGroup(
                AemConsoleExecuteAction().also { action ->
                    action.registerCustomShortcutSet(CommonShortcuts.getCtrlEnter(), fileEditor.component)
                },
                AemConsoleExecuteDistributedAction()
            )

            ag.add(SplitButtonAction(runActions))

            ag.addSeparator()
            ag.add(AemConsoleServerChooserAction(project))
        }

        val rightActionGroup = DefaultActionGroup().also { ag ->
            ag.add(object : AnAction("Open Configuration Dialog", "", AllIcons.General.Settings) {
                override fun actionPerformed(e: AnActionEvent) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(e.project, AemServersConfigurable::class.java)
                }
            })
        }


        val actionManager = ActionManager.getInstance()

        val leftToolbar =
            actionManager.createActionToolbar("LeftAemGroovyConsoleActionGroup", leftActionGroup, true).also {
                it.targetComponent = fileEditor.component
            }

        val rightToolbar =
            actionManager.createActionToolbar("RightAemGroovyConsoleActionGroup", rightActionGroup, true).also {
                it.targetComponent = fileEditor.component
            }

        this.add(leftToolbar.component, BorderLayout.WEST)

        this.add(rightToolbar.component, BorderLayout.EAST)
    }
}
