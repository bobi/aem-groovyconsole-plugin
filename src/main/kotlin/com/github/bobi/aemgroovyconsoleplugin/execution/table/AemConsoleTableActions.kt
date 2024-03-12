package com.github.bobi.aemgroovyconsoleplugin.execution.table

import com.github.bobi.aemgroovyconsoleplugin.utils.Notifications
import com.github.bobi.aemgroovyconsoleplugin.utils.openSaveFileDialog
import com.intellij.icons.AllIcons
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAware
import de.siegmar.fastcsv.writer.CsvWriter
import de.siegmar.fastcsv.writer.QuoteStrategies
import java.io.IOException


/**
 * User: Andrey Bardashevsky
 * Date/Time: 2024-03-04 23:00
 */
internal class ExportCsvAction(private val table: AemConsoleTable) :
    AnAction({ "Export to CSV" }, Presentation.NULL_STRING, AllIcons.Actions.Download),
    DumbAware {

    override fun update(e: AnActionEvent) {
        val project = e.project

        if (project != null) {
            e.presentation.isEnabled = table.model.rowCount > 0
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        val tableModel = table.model

        if (project != null && tableModel.rowCount > 0 && tableModel is AemConsoleTableModel) {
            val fileWrapper = openSaveFileDialog(project, "csv", "Export Table to CSV")

            fileWrapper?.let { vfw ->
                try {
                    CsvWriter.builder()
                        .quoteStrategy(QuoteStrategies.ALWAYS)
                        .build(vfw.file.toPath()).use { csv ->
                            csv.writeRecord(tableModel.table.columns.map { col -> col ?: "" })

                            tableModel.table.rows.forEach { row -> csv.writeRecord(row.map { cell -> cell ?: "" }) }
                        }

                    Notifications.notifyInfo("Table Exported to CSV", vfw.file.canonicalPath)
                } catch (e: IOException) {
                    Notifications.notifyError("Table Export Error", e.localizedMessage)
                }
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}

internal class FindAction(private val tablePanel: AemConsoleTablePanel) : AnAction(
    ActionsBundle.messagePointer("action.Find.text"), Presentation.NULL_STRING, AllIcons.Actions.Find
), DumbAware {
    init {
        ActionUtil.copyFrom(this, IdeActions.ACTION_FIND)
        registerCustomShortcutSet(shortcutSet, tablePanel)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        if (project != null) {
            tablePanel.showFindText()
        }
    }
}
