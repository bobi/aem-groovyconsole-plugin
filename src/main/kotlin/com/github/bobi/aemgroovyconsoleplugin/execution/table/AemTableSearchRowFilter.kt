package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.intellij.openapi.util.text.StringUtil
import javax.swing.RowFilter
import javax.swing.table.TableModel

/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-10 21:50
 */
internal class AemTableSearchRowFilter(private val table: AemConsoleTable) : RowFilter<TableModel, Int>() {

    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
        val searchSession = table.searchSession

        return if (searchSession != null && searchSession.filterRows && !StringUtil.isEmpty(searchSession.getFindModel().stringToFind)) {
            (0 until entry.valueCount).any { column -> searchSession.isMatchText(entry.getStringValue(column)) }
        } else {
            true
        }
    }
}
