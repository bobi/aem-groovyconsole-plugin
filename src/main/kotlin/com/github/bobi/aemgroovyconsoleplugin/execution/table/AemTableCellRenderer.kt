package com.github.bobi.aemgroovyconsoleplugin.execution.table

import java.awt.Color
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-10 18:49
 */
class AemTableCellRenderer : DefaultTableCellRenderer.UIResource() {

    internal var defaultCellRenderer: TableCellRenderer? = null

    init {
        border = AemConsoleTable.colorScheme.defaultCellEmptyBorder
        noFocusBorder = AemConsoleTable.colorScheme.defaultCellEmptyBorder
    }

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val cellRenderer = defaultCellRenderer

        if (table == null || cellRenderer == null) {
            return this
        }

        val component = cellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        if (component is JComponent) {
            component.border = AemConsoleTable.colorScheme.defaultCellEmptyBorder
        }

        val searchMatchedCell =
            (table as? AemConsoleTable)?.let { t -> t.searchSession?.isMatchText(value.toString()) } ?: false

        val lastSelectedCell = (table.selectionModel.leadSelectionIndex == row)
                && (table.columnModel.selectionModel.leadSelectionIndex == column)

        component.background = getCellBackground(table, isSelected, lastSelectedCell, searchMatchedCell)
        component.foreground = getCellForeground(table, isSelected, lastSelectedCell)


        return component
    }

    private fun getCellBackground(
        table: JTable,
        isSelected: Boolean,
        hasFocus: Boolean,
        searchMatchedCell: Boolean
    ): Color {
        return when {
            hasFocus && isSelected -> {
                AemConsoleTable.colorScheme.cellFocusingBackground
            }

            searchMatchedCell -> {
                AemConsoleTable.colorScheme.cellSearchBackground
            }

            isSelected -> {
                table.selectionBackground
            }

            else -> {
                table.background
            }
        }
    }

    private fun getCellForeground(
        table: JTable,
        isSelected: Boolean,
        hasFocus: Boolean,
    ): Color {
        return when {
            hasFocus && isSelected -> {
                AemConsoleTable.colorScheme.cellFocusingForeground
            }

            isSelected -> {
                table.selectionForeground
            }

            else -> {
                table.foreground
            }
        }
    }
}
