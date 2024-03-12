package com.github.bobi.aemgroovyconsoleplugin.execution.table

import javax.swing.SortOrder
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter


/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-06 18:10
 */
class AemConsoleTableSorter<M : TableModel>(model: M) : TableRowSorter<M>(model) {

    private fun checkColumn(column: Int) {
        if (column < 0 || column >= modelWrapper.columnCount) {
            throw IndexOutOfBoundsException(
                "column beyond range of TableModel"
            )
        }
    }

    override fun toggleSortOrder(column: Int) {
        checkColumn(column)

        if (isSortable(column)) {
            var keys: MutableList<SortKey> = ArrayList(sortKeys)

            val sortKey: SortKey
            var sortIndex = keys.size - 1

            while (sortIndex >= 0) {
                if (keys[sortIndex].column == column) {
                    break
                }
                sortIndex--
            }

            when (sortIndex) {
                -1 -> {
                    // Key doesn't exist
                    sortKey = SortKey(column, SortOrder.ASCENDING)
                    keys.add(0, sortKey)
                }

                0 -> {
                    // It's the primary sorting key, toggle it
                    keys[0] = toggle(keys[0])
                }

                else -> {
                    // It's not the first, but was sorted on, remove old
                    // entry, insert as first with ascending.
                    keys.removeAt(sortIndex)
                    keys.add(0, SortKey(column, SortOrder.ASCENDING))
                }
            }

            if (keys.size > maxSortKeys) {
                keys = keys.subList(0, maxSortKeys)
            }

            sortKeys = keys
        }
    }

    private fun toggle(key: SortKey): SortKey {
        val nextSortOrder = when (key.sortOrder) {
            SortOrder.ASCENDING -> SortOrder.DESCENDING
            SortOrder.DESCENDING -> SortOrder.UNSORTED
            SortOrder.UNSORTED -> SortOrder.ASCENDING
            else -> SortOrder.UNSORTED
        }

        return SortKey(key.column, nextSortOrder)
    }
}
