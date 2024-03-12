package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ui.components.BorderLayoutPanel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-05 23:17
 */
class AemConsoleTablePanel(internal val project: Project) : BorderLayoutPanel() {

    internal val table = AemConsoleTable()

    private var findComponent: AemConsoleTableSearchSession? = null

    init {
        addToCenter(ScrollPaneFactory.createScrollPane(table).also {
            it.setColumnHeaderView(table.tableHeader)
            it.setRowHeaderView(RowNumberColumn(table))
        })
    }

    internal fun showFindText() {
        var searchComponent = findComponent

        if (searchComponent == null) {
            searchComponent = AemConsoleTableSearchSession(project, table)

            this.findComponent = searchComponent
            table.searchSession = searchComponent
            val component = searchComponent.getComponent()

            addToTop(component)

            searchComponent.requestFocus()

            val listener: AemConsoleTableSearchComponentListener = object : AemConsoleTableSearchComponentListener {
                override fun searchSessionUpdated() {
                    table.searchSessionUpdated()
                }

                override fun hideSearchComponent() {
                    remove(component)
                    table.searchSession = null
                    findComponent = null
                    table.searchSessionUpdated()
                    table.requestFocus()
                }
            }

            searchComponent.addListener(listener)
        } else {
            searchComponent.requestFocus()
        }
    }
}
