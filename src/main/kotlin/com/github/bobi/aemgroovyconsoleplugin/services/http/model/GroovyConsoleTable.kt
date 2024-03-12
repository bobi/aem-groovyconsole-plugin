package com.github.bobi.aemgroovyconsoleplugin.services.http.model

/**
 * User: Andrey Bardashevsky
 * Date/Time: 06.08.2022 18:51
 */
data class OutputTable(val table: GroovyConsoleTable)

data class GroovyConsoleTable(val columns: List<String?>, val rows: List<List<String?>>)
