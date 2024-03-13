package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.find.*
import com.intellij.find.editorHeaderActions.*
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.Rectangle
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

internal class AemConsoleTableSearchSession(
    private val project: Project,
    private val table: AemConsoleTable
) : SearchSession, DataProvider {

    private val searchComponent = createSearchComponent()

    private val findModel = createFindModel()

    private val findManager = FindManager.getInstance(project)

    var filterRows: Boolean = false

    private val listeners: MutableList<AemConsoleTableSearchComponentListener> = CopyOnWriteArrayList()

    private val eventMulticaster: AemConsoleTableSearchComponentListener =
        object : AemConsoleTableSearchComponentListener {
            override fun searchSessionUpdated() {
                for (listener in listeners) {
                    listener.searchSessionUpdated()
                }
            }

            override fun hideSearchComponent() {
                for (listener in listeners) {
                    listener.hideSearchComponent()
                }
            }
        }

    init {
        searchComponent.searchTextComponent.text = findModel.stringToFind
        searchComponent.searchTextComponent.selectAll()

        searchComponent.searchTextComponent.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(keyEvent: KeyEvent) {
                if (keyEvent.keyCode == KeyEvent.VK_ESCAPE) {
                    eventMulticaster.hideSearchComponent()
                } else if (keyEvent.keyCode != KeyEvent.VK_ENTER && keyEvent.keyCode != KeyEvent.VK_DOWN) {
                    if (keyEvent.keyCode == KeyEvent.VK_UP) {
                        searchBackward()
                    }
                } else {
                    searchForward()
                }
            }
        })
    }

    private fun createFindModel(): FindModel {
        return FindModel().apply {
            copyFrom(FindManager.getInstance(project).findInFileModel)

            FindUtil.configureFindModel(false, this, false, getSelectedText())

            addObserver {
                val stringToFind = findModel.stringToFind
                val incorrectRegex = findModel.isRegularExpressions && findModel.compileRegExp() == null
                val hasMatches = hasMatches()

                if (!incorrectRegex && (!StringUtil.isNotEmpty(stringToFind) || hasMatches)) {
                    searchComponent.setRegularBackground()
                } else {
                    searchComponent.setNotFoundBackground()
                }

                searchComponent.statusText = if (incorrectRegex) FindBundle.message("find.incorrect.regexp") else ""

                searchComponent.update(stringToFind, "", false, findModel.isMultiline)

                if (hasMatches) {
                    selectOccurrence(selectStart = true, next = true)
                }

                FindUtil.updateFindInFileModel(project, findModel, true)

                eventMulticaster.searchSessionUpdated()
            }
        }
    }

    private fun getSelectedText(): String? {
        val selectedRows = table.selectedRows
        val selectedColumns = table.selectedColumns

        return if (selectedRows.size == 1 && selectedColumns.size == 1) {
            table.getText(selectedRows[0], selectedColumns[0])
        } else {
            return null
        }
    }

    override fun getFindModel(): FindModel = findModel

    override fun getComponent(): SearchReplaceComponent = searchComponent

    override fun hasMatches(): Boolean = findOccurrence(includeStart = true, forward = true) != null

    override fun getData(dataId: String): Any? {
        return if (SearchSession.KEY.`is`(dataId)) this else null
    }

    override fun searchForward() = selectOccurrence(selectStart = false, next = true)

    override fun searchBackward() = selectOccurrence(selectStart = false, next = false)

    override fun close() {
        eventMulticaster.hideSearchComponent()

        IdeFocusManager.getInstance(project).requestFocus(table, false)
    }

    internal fun requestFocus() {
        searchComponent.requestFocusInTheSearchFieldAndSelectContent(project)
    }

    private fun createSearchComponent(): SearchReplaceComponent {
        return SearchReplaceComponent
            .buildFor(project, table)
            .addPrimarySearchActions(PrevOccurrenceAction(), NextOccurrenceAction())
            .addExtraSearchActions(
                ToggleMatchCase(),
                ToggleRegex(),
                ToggleWholeWordsOnlyAction(),
                ToggleFilteringAction(),
                StatusTextAction()
            )
            .withCloseAction { close() }
            .withDataProvider(this)
            .build().also {
                it.addListener(object : SearchReplaceComponent.Listener {
                    override fun searchFieldDocumentChanged() {
                        val text = searchComponent.searchTextComponent.text
                        findModel.stringToFind = text
                        findModel.isMultiline = text.contains("\n")
                        eventMulticaster.searchSessionUpdated()
                    }

                    override fun multilineStateChanged() {
                        findModel.isMultiline = searchComponent.isMultiline
                    }

                    override fun replaceFieldDocumentChanged() = Unit
                })
            }
    }

    fun addListener(listener: AemConsoleTableSearchComponentListener) {
        listeners.add(listener)
    }

    private fun selectOccurrence(selectStart: Boolean, next: Boolean) {
        val cell = findOccurrence(selectStart, next)

        if (cell != null) {
            table.columnModel.selectionModel.setSelectionInterval(cell.second, cell.second)
            table.selectionModel.setSelectionInterval(cell.first, cell.first)
            table.scrollRectToVisible(Rectangle(table.getCellRect(cell.first, 0, true)))
        }
    }

    private fun findOccurrence(includeStart: Boolean, forward: Boolean): Pair<Int, Int>? {
        val rowCount = table.rowCount
        val columnCount = table.columnCount

        if (rowCount > 0 && columnCount > 0) {
            var startRow = table.selectedRow
            var startColumn = table.selectedColumn

            if (startRow == -1 || startColumn == -1) {
                startRow = 0
                startColumn = 0
            }

            val range = getSearchCellsRange(startRow, startColumn, rowCount, columnCount, forward, includeStart)

            for (i in range) {
                val row = i / columnCount
                val col = i % columnCount

                if (isMatchText(table.getText(row, col))) {
                    return Pair(row, col)
                }
            }
        }

        return null
    }

    private fun getSearchCellsRange(
        startRow: Int,
        startColumn: Int,
        rowCount: Int,
        columnCount: Int,
        forward: Boolean,
        includeStartCell: Boolean
    ): Sequence<Int> {
        val tableRange = 0 until rowCount * columnCount

        val startCell = min(tableRange.last, max(tableRange.first, startRow * columnCount + startColumn))

        val step = if (forward) 1 else -1
        val advance = if (includeStartCell) 0 else step

        val rangeStart = startCell + advance
        val rangeEnd = rangeStart - step

        var seq = emptySequence<Int>()

        if (forward) {
            if (rangeStart in tableRange) {
                seq += IntProgression.fromClosedRange(rangeStart, tableRange.last, step).asSequence()
            }
            if (rangeEnd in tableRange) {
                seq += IntProgression.fromClosedRange(tableRange.first, rangeEnd, step).asSequence()
            }
        } else {
            if (rangeStart in tableRange) {
                seq += IntProgression.fromClosedRange(rangeStart, tableRange.first, step).asSequence()
            }
            if (rangeEnd in tableRange) {
                seq += IntProgression.fromClosedRange(tableRange.last, rangeEnd, step).asSequence()
            }
        }

        return seq
    }

    fun isMatchText(text: String): Boolean {
        return findManager.findString(text, 0, findModel).isStringFound
    }

    private inner class ToggleFilteringAction :
        EditorHeaderToggleAction("Filter rows") {

        override fun isSelected(session: SearchSession): Boolean {
            return this@AemConsoleTableSearchSession.filterRows
        }

        override fun setSelected(session: SearchSession, selected: Boolean) {
            val oldVal: Boolean = this@AemConsoleTableSearchSession.filterRows

            this@AemConsoleTableSearchSession.filterRows = selected
            if (oldVal != selected) {
                this@AemConsoleTableSearchSession.eventMulticaster.searchSessionUpdated()
            }
        }
    }
}

interface AemConsoleTableSearchComponentListener {
    fun searchSessionUpdated()

    fun hideSearchComponent()
}
