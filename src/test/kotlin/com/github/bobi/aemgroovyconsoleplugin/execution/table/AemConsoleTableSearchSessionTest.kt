package com.github.bobi.aemgroovyconsoleplugin.execution.table

import org.junit.Assert.assertEquals
import org.junit.Test


/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-15 20:06
 */

/* Table:
    0	1	2	3	4
    5	6	7	8	9
    10	11	12	13	14
    15	16	17	18	19
    20	21	22	23	24
 */

class AemConsoleTableSearchSessionTest {
    private val rowCount = 5

    private val columnCount = 5

    @Test
    fun getSearchCellsRangeForwardIncludeStart() {
        val forward = true
        val includeStartCell = true

        assertEquals(
            ((6..24) + (0..5)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                1, 1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            (0..24).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                -1, -1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            ((24..24) + (0..23)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                10, 10, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )
    }

    @Test
    fun getSearchCellsRangeBackwardIncludeStart() {
        val forward = false
        val includeStartCell = true

        assertEquals(
            ((6 downTo 0) + (24 downTo 7)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                1, 1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            ((0..0) + (24 downTo 1)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                -1, -1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            (24 downTo 0).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                10, 10, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )
    }

    @Test
    fun getSearchCellsRangeForwardSkipStart() {
        val forward = true
        val includeStartCell = false

        assertEquals(
            ((7..24) + (0..6)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                1, 1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            ((1..24) + (0..0)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                -1, -1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            (0..24).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                10, 10, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )
    }

    @Test
    fun getSearchCellsRangeBackwardSkipStart() {
        val forward = false
        val includeStartCell = false

        assertEquals(
            ((5 downTo 0) + (24 downTo 6)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                1, 1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            (24 downTo 0).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                -1, -1, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )

        assertEquals(
            ((23 downTo 0) + (24..24)).toList(),
            AemConsoleTableSearchSession.getSearchCellsRange(
                10, 10, rowCount, columnCount, forward, includeStartCell
            ).toList(),
        )
    }
}
