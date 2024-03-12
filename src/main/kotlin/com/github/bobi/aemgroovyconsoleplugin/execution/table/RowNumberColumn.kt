package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.util.ui.GraphicsUtil
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.CellRendererPane
import javax.swing.JComponent
import javax.swing.JTable
import kotlin.math.max

internal class RowNumberColumn(private val table: AemConsoleTable) : JComponent() {
    private val rendererPane = CellRendererPane()

    private var preferredWidth: Int = 1

    private val cellRenderer = RowNumberCellRenderer(table)

    init {
        this.add(rendererPane)
        initListeners(table)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(preferredWidth, table.height)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D

        GraphicsUtil.setupAntialiasing(g)

        super.paintComponent(g)

        val table = table
        val clip = g.getClipBounds()
        val rowMargin = table.rowMargin
        val gridLineStroke = BasicStroke(rowMargin.toFloat())

        processVisibleRows(table, g) { row, y ->
            val rowHeight = table.getRowHeight(row)
            val cellHeight = max(0.0, (rowHeight - rowMargin).toDouble()).toInt()

            cellRenderer.row = row

            rendererPane.paintComponent(
                g2,
                this@RowNumberColumn.cellRenderer,
                this@RowNumberColumn,
                0,
                y,
                width,
                cellHeight,
                true
            )
            val gridY = y + cellHeight + rowMargin / 2
            val backupStroke = g2.stroke
            val backupColor = g2.color

            g2.stroke = gridLineStroke
            g2.color = table.gridColor

            drawHLine(g2, clip.x, clip.x + clip.width, gridY)

            g2.stroke = backupStroke
            g2.color = backupColor

            return@processVisibleRows true
        }
    }

    private fun initListeners(table: JTable) {
        table.addPropertyChangeListener("model") { this.updatePreferredSize() }
        table.selectionModel.addListSelectionListener { this.repaint() }

        table.addComponentListener(object : ComponentAdapter() {
            var previousTableHeight: Int = 0

            override fun componentResized(e: ComponentEvent) {
                val c = e.component
                if (previousTableHeight != c.height) {
                    this@RowNumberColumn.updatePreferredSize(this@RowNumberColumn.preferredSize.width, false)
                    previousTableHeight = c.height
                }
            }
        })
    }

    private fun updatePreferredSize() {
        val insets = this.insets
        this.updatePreferredSize(insets.left + insets.right + calcPreferredWidthWithoutInsets(), true)
    }

    private fun updatePreferredSize(preferredWidth: Int, checkMinWidth: Boolean) {
        var newPreferredWidth = preferredWidth

        if (checkMinWidth && newPreferredWidth < MIN_PREFERRED_WIDTH) {
            newPreferredWidth = MIN_PREFERRED_WIDTH
        }

        val prevSize = this.preferredSize
        this.preferredWidth = newPreferredWidth
        val newSize = this.preferredSize
        this.firePropertyChange("preferredSize", prevSize, newSize)
    }

    private fun calcPreferredWidthWithoutInsets(): Int {
        var maxCellRendererWidth = 0

        for (row in 0 until table.model.rowCount) {
            cellRenderer.row = row

            val preferredSize = cellRenderer.preferredSize
            if (preferredSize.width > maxCellRendererWidth) {
                maxCellRendererWidth = preferredSize.width
            }
        }

        return max(1.0, maxCellRendererWidth.toDouble()).toInt()
    }

    override fun getBackground(): Color {
        return AemConsoleTable.colorScheme.backgroundColor
    }

    companion object {
        private const val MIN_PREFERRED_WIDTH = 15

        private fun drawHLine(g: Graphics, x1: Int, x2: Int, y: Int) {
            var newX1 = x1
            var newX2 = x2

            if (newX2 < newX1) {
                val temp = newX2
                newX2 = newX1
                newX1 = temp
            }

            g.fillRect(newX1, y, newX2 - newX1 + 1, 1)
        }

        private fun processVisibleRows(table: JTable, g: Graphics, proc: (row: Int, y: Int) -> Boolean) {
            val clip = g.clipBounds
            val rowCount = table.rowCount
            var startY = table.rowMargin / 2

            var y: Int
            var startRow = 0

            while (startRow < rowCount) {
                y = table.getRowHeight(startRow)
                if (startY + y >= clip.y) {
                    break
                }

                startY += y
                ++startRow
            }

            y = startY

            var row = startRow

            while (row < rowCount && y <= clip.y + clip.height && proc(row, y)) {
                val rowHeight = table.getRowHeight(row)
                y += rowHeight
                ++row
            }
        }
    }
}
