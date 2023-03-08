package com.github.bobi.aemgroovyconsoleplugin.execution

import com.github.bobi.aemgroovyconsoleplugin.lang.AemConsoleLanguageFileType
import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentDescriptorReusePolicy
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import javax.swing.JComponent

/**
 * User: Andrey Bardashevsky
 * Date/Time: 19.08.2022 18:28
 */
class AemConsoleRunContentDescriptor(
    project: Project,
    val contentFile: VirtualFile,
    val config: AemServerConfig,
    val console: ConsoleView,
    val action: GroovyConsoleHttpService.Action,
    component: JComponent,
    tmpFolder: File,
) : RunContentDescriptor(
    console,
    AemGroovyConsoleProcessHandler(project, contentFile, config, action),
    component,
    "[${config.name}] - [${contentFile.name}]",
    AemConsoleLanguageFileType.icon
) {

    val tmpFile: File by lazy {
        FileUtil.createTempFile(tmpFolder, "output", null)
    }

    init {
        reusePolicy = object : RunContentDescriptorReusePolicy() {
            override fun canBeReusedBy(newDescriptor: RunContentDescriptor): Boolean {
                return if (newDescriptor is AemConsoleRunContentDescriptor) {
                    contentFile == newDescriptor.contentFile && config.id == newDescriptor.config.id
                } else {
                    false
                }
            }
        }

        processHandler!!.addProcessListener(ProcessLogOutputListener(tmpFile), console)

        console.attachToProcess(processHandler!!)
    }
}