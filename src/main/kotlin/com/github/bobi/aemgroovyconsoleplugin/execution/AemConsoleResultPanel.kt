package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.execution.table.AemConsoleTableModel
import com.github.bobi.aemgroovyconsoleplugin.execution.table.AemConsoleTablePanel
import com.github.bobi.aemgroovyconsoleplugin.execution.table.ExportCsvAction
import com.github.bobi.aemgroovyconsoleplugin.execution.table.FindAction
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.util.ui.components.BorderLayoutPanel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-03 19:35
 */

class AemConsoleResultPanel(val project: Project, val console: ConsoleView) :
    BorderLayoutPanel(), Disposable {

    private val tabs = JBTabsFactory.createTabs(project, this)

    private val tabsPresentation = tabs.presentation

    private val tablePanel = AemConsoleTablePanel(project)

    private val tableTabPanel = BorderLayoutPanel().also { panel ->
        val consoleComponent = console.component

        val toolbarActions = DefaultActionGroup().also { tbActions ->
            tbActions.add(RerunAction(consoleComponent))
            tbActions.add(FindAction(tablePanel))
            tbActions.add(ExportCsvAction(tablePanel.table))
            tbActions.add(CloseAction(consoleComponent))
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar("AemGroovyConsoleTableExecutorToolbar", toolbarActions, false).also {
                it.targetComponent = tablePanel
            }

        panel.addToCenter(tablePanel)
        panel.addToLeft(toolbar.component)
    }

    private val tableTab = TabInfo(tableTabPanel).also {
        it.setText("Table")
        it.setIcon(AllIcons.Nodes.DataTables)
    }

    private val consoleTabPanel = BorderLayoutPanel().also { panel ->
        val consoleComponent = console.component

        val toolbarActions = DefaultActionGroup().also { tbActions ->
            tbActions.add(RerunAction(consoleComponent))
            tbActions.addAll(*console.createConsoleActions())
            tbActions.add(SaveOutputAction())
            tbActions.add(CloseAction(consoleComponent))
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar("AemGroovyConsoleExecutorToolbar", toolbarActions, false).also {
                it.targetComponent = consoleComponent
            }

        panel.addToCenter(consoleComponent)
        panel.addToLeft(toolbar.component)
    }

    private val consoleTab = TabInfo(consoleTabPanel).also {
        it.setText("Console")
        it.setIcon(AllIcons.Nodes.Console)
    }

    init {
        tabsPresentation.setTabDraggingEnabled(false)

        tabs.addTab(consoleTab)
        tabs.addTab(tableTab)
        tabsPresentation.isHideTabs = true

        addToCenter(tabs.component)
    }

    fun attachToProcess(processHandler: ProcessHandler) {
        tabsPresentation.isHideTabs = true

        tabs.select(consoleTab, true)

        console.attachToProcess(processHandler)

        processHandler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                processHandler.removeProcessListener(this)
            }

            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType === AemConsoleOutputType.TABLE_OUTPUT_TYPE) {
                    tablePanel.table.model = AemConsoleTableModel.fromJsonTable(event.text)
                    tabsPresentation.isHideTabs = false

                    tabs.select(tableTab, true)
                }
            }
        })
    }

    override fun dispose() {
    }
}

