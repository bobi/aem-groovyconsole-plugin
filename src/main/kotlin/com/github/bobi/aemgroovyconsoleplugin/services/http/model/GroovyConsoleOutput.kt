package com.github.bobi.aemgroovyconsoleplugin.services.http.model

data class GroovyConsoleOutput(
    val result: String? = null,
    val output: String? = null,
    val runningTime: String? = null,
    val exceptionStackTrace: String? = null,
    val table: GroovyConsoleTable? = null
)
