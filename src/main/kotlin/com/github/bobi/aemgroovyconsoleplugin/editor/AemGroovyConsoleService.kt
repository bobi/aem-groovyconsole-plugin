package com.github.bobi.aemgroovyconsoleplugin.editor

import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.addConsole
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getConsole
import com.github.bobi.aemgroovyconsoleplugin.editor.GroovyConsoleUserData.getCurrentAemConfig
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * User: Andrey Bardashevsky
 * Date/Time: 07.08.2022 18:33
 */
class AemGroovyConsoleService(private val project: Project) {

    private val executor = DefaultRunExecutor.getRunExecutorInstance()

    fun execute(contentFile: VirtualFile) {
        val currentServerConfig = contentFile.getCurrentAemConfig(project) ?: return

        var console = contentFile.getConsole(currentServerConfig.id)

        if (console != null) {
            console.toFrontRunContent()
        } else {
            console = AEMGroovyConsole(project, contentFile, executor)

            contentFile.addConsole(currentServerConfig.id, console)

            console.showRunContent()
        }

        console.executeScript()
    }

    companion object {
        fun getInstance(project: Project): AemGroovyConsoleService = project.service()
    }
}