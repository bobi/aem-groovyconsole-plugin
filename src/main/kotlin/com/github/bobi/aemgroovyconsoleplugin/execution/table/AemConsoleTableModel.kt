package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleTable
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.OutputTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableModel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-03 16:00
 */
internal class AemConsoleTableModel(internal val table: GroovyConsoleTable) : AbstractTableModel() {
    override fun getRowCount(): Int {
        return table.rows.size
    }

    override fun getColumnCount(): Int {
        return table.columns.size
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        return table.rows[rowIndex][columnIndex] ?: ""
    }

    override fun getColumnName(column: Int): String {
        return table.columns[column] ?: super.getColumnName(column)
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return String::class.java
    }


    companion object {
        private val gson: Gson = GsonBuilder().create()

        fun fromJsonTable(jsonTable: String): TableModel {
            val table = gson.fromJson(jsonTable, OutputTable::class.java)

            return AemConsoleTableModel(table.table)
        }
    }
}
