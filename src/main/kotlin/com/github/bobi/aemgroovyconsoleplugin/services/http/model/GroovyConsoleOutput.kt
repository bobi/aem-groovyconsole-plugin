package com.github.bobi.aemgroovyconsoleplugin.services.http.model

data class GroovyConsoleOutput(val output: String, val runningTime: String, val exceptionStackTrace: String, val table: GroovyConsoleTable? = null)