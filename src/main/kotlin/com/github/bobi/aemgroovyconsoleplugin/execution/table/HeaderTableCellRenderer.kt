package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.icons.AllIcons
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer

internal class HeaderTableCellRenderer(private val defaultHeaderRenderer: TableCellRenderer) : TableCellRenderer {
    private val panel = BorderLayoutPanel()
        .andTransparent()
        .addToLeft(JLabel(AllIcons.Nodes.DataColumn))

    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val delegate =
            defaultHeaderRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

        return if (delegate is JLabel) {
            panel.addToCenter(delegate)
        } else {
            delegate
        }
    }
}
