package com.github.bobi.aemgroovyconsoleplugin.config.ui

import com.github.bobi.aemgroovyconsoleplugin.config.SettingsChangedNotifier
import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.PlatformIcons
import javax.swing.JComponent


class AemServersConfigurable(private val project: Project) : Configurable {

    private val persistentStateService = PersistentStateService.getInstance(project)

    private val aemStoredServers: Collection<AemServerConfig>
        get() {
            return persistentStateService.getAEMServers()
        }

    private val persistedAemConfigs: MutableList<AemServerConfigUI>

    init {
        persistedAemConfigs = readConfigs().toMutableList()
    }

    private val table: AemServersTable = AemServersTable(persistedAemConfigs.map { it.copy() })

    private fun editSelectedRow() {
        val selectedRow = table.selectedRow

        if (selectedRow >= 0) {
            val model = table.getRow(selectedRow)

            if (AemServerEditDialog(project, model).showAndGet()) {
                table.model.fireTableRowsUpdated(selectedRow, selectedRow)
            }
        }
    }

    override fun createComponent(): JComponent = panel {
        table.addDoubleClickListener { editSelectedRow() }

        val toolbarDecorator = ToolbarDecorator.createDecorator(table).also { toolbar ->
            toolbar.setAddAction {
                val model = AemServerConfigUI()

                if (AemServerEditDialog(project, model).showAndGet()) {
                    table.model.addRow(model)
                }
            }

            toolbar.setRemoveAction {
                val selectedRow = table.selectedRow

                if (selectedRow >= 0) {
                    table.model.removeRow(table.convertRowIndexToModel(selectedRow))
                }
            }

            toolbar.setEditAction {
                editSelectedRow()
            }

            toolbar.addExtraAction(
                object : ToolbarDecorator.ElementActionButton("Duplicate", "Duplicate", PlatformIcons.COPY_ICON) {
                    override fun actionPerformed(e: AnActionEvent) {
                        val selectedRow = table.selectedRow
                        if (selectedRow >= 0) {
                            val model = table.getRow(selectedRow)

                            table.model.addRow(model.duplicate())
                        }
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread {
                        return ActionUpdateThread.EDT
                    }
                }.also { action ->
                    action.setShortcut(CommonShortcuts.getDuplicate())
                } as AnAction
            )
        }

        row {
            cell(toolbarDecorator.createPanel())
                .align(Align.FILL)
                .resizableColumn()
        }.resizableRow()
    }

    override fun getDisplayName(): String {
        return "AEM Groovy Console"
    }

    override fun isModified(): Boolean {
        if (persistedAemConfigs.size != table.model.items.size) {
            return true
        }

        for ((index, value) in persistedAemConfigs.withIndex()) {
            if (value != table.model.items[index]) {
                return true
            }
        }

        return false
    }

    override fun apply() {
        persistedAemConfigs.forEach {
            PasswordsService.removeCredentials(it.id)
        }

        val items = table.model.items

        persistentStateService.setAEMServers(items.map { it.toAemServerConfig() })

        items.forEach {
            PasswordsService.setCredentials(it.id, it.user, it.password)
        }

        val newConfigs = readConfigs()

        persistedAemConfigs.clear()
        persistedAemConfigs.addAll(newConfigs)

        project.messageBus.syncPublisher(SettingsChangedNotifier.TOPIC).settingsChanged()
    }

    override fun reset() {
        table.model.items = persistedAemConfigs.map { it.copy() }
    }

    private fun readConfigs(): List<AemServerConfigUI> {
        val result = mutableListOf<AemServerConfigUI>()

        for (aemServer in aemStoredServers) {
            if (aemServer.id > 0) {
                val credentials = PasswordsService.getCredentials(aemServer.id)

                result.add(AemServerConfigUI.fromAemServerConfig(aemServer, credentials))
            }
        }

        return result
    }
}
