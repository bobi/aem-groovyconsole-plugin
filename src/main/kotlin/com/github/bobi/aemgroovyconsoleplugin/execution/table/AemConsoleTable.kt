package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.ui.table.JBTable
import java.awt.Color
import java.awt.Font
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import kotlin.math.ceil


/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-03 16:01
 */
class AemConsoleTable : JBTable() {

    private val cellRenderer = AemTableCellRenderer()

    private var rowFilter = AemTableSearchRowFilter(this)

    internal var searchSession: AemConsoleTableSearchSession? = null

    init {
        cellSelectionEnabled = true
        rowSelectionAllowed = true
        isStriped = false
        setEnableAntialiasing(true)
        setShowGrid(true)

        tableHeader.reorderingAllowed = false
        tableHeader.defaultRenderer = HeaderTableCellRenderer(tableHeader.defaultRenderer)

        setFonts()
    }

    override fun getDefaultRenderer(columnClass: Class<*>?): TableCellRenderer? {
        val tableCellRenderer = super.getDefaultRenderer(columnClass)

        return if (tableCellRenderer != null && tableCellRenderer != cellRenderer) {
            cellRenderer.apply { defaultCellRenderer = tableCellRenderer }
        } else {
            tableCellRenderer
        }
    }

    override fun setModel(model: TableModel) {
        super.setModel(model)
        rowSorter = AemConsoleTableSorter(model).also { it.rowFilter = rowFilter }
    }

    internal fun getText(rowIndex: Int, columnIndex: Int): String {
        return model.getValueAt(convertRowIndexToModel(rowIndex), convertColumnIndexToModel(columnIndex)).toString()
    }

    internal fun searchSessionUpdated() {
        rowSorter?.allRowsChanged()

        repaint()
    }

    private fun setFonts() {
        val newFont: Font = colorScheme.getScaledFont()
        font = newFont

        setRowHeight(getRowHeight())

        val tableHeader = getTableHeader()
        if (tableHeader != null) tableHeader.font = newFont
    }

    internal fun getTextLineHeight(): Int {
        return ceil((getFontMetrics(font).height * colorScheme.lineSpacing)).toInt()
    }

    override fun getRowHeight(): Int {
        return getTextLineHeight() + getRowMargin() + (if ((isStriped || !this.showHorizontalLines)) 1 else 0)
    }

    override fun getBackground(): Color {
        return colorScheme.backgroundColor
    }

    override fun getSelectionBackground(): Color {
        return colorScheme.selectionBackground
    }

    override fun getSelectionForeground(): Color {
        return colorScheme.selectionForeground
    }

    override fun getGridColor(): Color {
        return colorScheme.gridColor
    }

    override fun getHoveredRowBackground(): Color? = null

    companion object {
        internal val colorScheme = AemConsoleTableColorScheme()
    }
}
