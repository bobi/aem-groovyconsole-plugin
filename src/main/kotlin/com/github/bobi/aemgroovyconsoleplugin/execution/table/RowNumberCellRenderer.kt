package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.ui.CellRendererPanel
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.BorderFactory

internal class RowNumberCellRenderer(private val table: AemConsoleTable) : CellRendererPanel() {
    var row: Int? = null
        set(value) {
            field = value

            isSelected = (value != null) && table.isRowSelected(value)
        }

    init {
        border = BorderFactory.createCompoundBorder(
            object : CustomLineBorder(
                null, 0, 0, 0, 1
            ) {
                override fun getColor(): Color {
                    return AemConsoleTable.colorScheme.gridColor
                }
            }, JBUI.Borders.empty(0, 8)
        )
    }

    override fun getPreferredSize(): Dimension {
        val rowNum = row

        return if (rowNum != null) {
            val preferredWidth = getPreferredWidth()

            Dimension(preferredWidth, table.getRowHeight(rowNum))
        } else {
            Dimension(0, 0)
        }
    }

    private fun getPreferredWidth(): Int {
        val insets = this.insets
        val width = table.getFontMetrics(table.font).stringWidth(getRowNumString())
        return width + insets.right + insets.left
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        paintBackground(g)
        paintLineNumber(g)
    }

    private fun paintLineNumber(g: Graphics) {
        val fontBackup = g.font

        g.font = table.font

        val insets = this.insets
        val middleX = insets.left + (width - insets.left - insets.right) / 2

        val rowNum: String = getRowNumString()

        val stringWidth = g.fontMetrics.stringWidth(rowNum)

        val metrics: FontMetrics = table.getFontMetrics(table.font)
        val baseline = (table.getTextLineHeight() + metrics.ascent - metrics.descent) / 2

        g.color =
            if (isSelected) AemConsoleTable.colorScheme.selectionForeground else AemConsoleTable.colorScheme.labelDisabledForegroundColor

        g.drawString(rowNum, middleX - stringWidth / 2, baseline)

        g.font = fontBackup
    }

    private fun paintBackground(g: Graphics) {
        val backup = g.color
        g.color =
            if (isSelected) AemConsoleTable.colorScheme.selectionBackground else AemConsoleTable.colorScheme.backgroundColor
        g.fillRect(0, 0, width, height)
        g.color = backup
    }

    private fun getRowNumString(): String {
        val rowNum = row

        return if (rowNum != null) {
            (rowNum + 1).toString()
        } else {
            ""
        }
    }
}
