package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.services.PersistentStateService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

/**
 * User: Andrey Bardashevsky
 * Date/Time: 30.07.2022 15:10
 */
object GroovyConsoleUserData {

    private val GROOVY_CONSOLE_CURRENT_SERVER_ID = Key.create<Long>("AEMGroovyConsoleCurrentServerId")

    fun VirtualFile.getCurrentAemServerId(): Long? = getUserData(GROOVY_CONSOLE_CURRENT_SERVER_ID)

    fun VirtualFile.setCurrentAemServerId(id: Long?) {
        putUserData(GROOVY_CONSOLE_CURRENT_SERVER_ID, id)
    }

    internal fun VirtualFile.getCurrentAemConfig(
        project: Project
    ): AemServerConfig? {
        val serverId = getCurrentAemServerId()

        if (serverId != null) {
            return PersistentStateService.getInstance(project).findById(serverId)
        }

        return null
    }
}