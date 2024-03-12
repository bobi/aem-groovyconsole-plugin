package com.github.bobi.aemgroovyconsoleplugin.execution

import com.intellij.execution.filters.ConsoleDependentInputFilterProvider
import com.intellij.execution.filters.InputFilter
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.search.GlobalSearchScope

class ConsoleInputFilterProvider : ConsoleDependentInputFilterProvider() {
    override fun getDefaultFilters(
        consoleView: ConsoleView,
        project: Project,
        globalSearchScope: GlobalSearchScope
    ): List<InputFilter> {
        return listOf(TableOutputFilter())
    }
}

private class TableOutputFilter : InputFilter {
    override fun applyFilter(
        text: String,
        contentType: ConsoleViewContentType
    ): MutableList<Pair<String, ConsoleViewContentType>>? {
        if (contentType == AemConsoleOutputType.TABLE_OUTPUT) {
            return mutableListOf(Pair(null, contentType))
        }

        return null
    }

}
