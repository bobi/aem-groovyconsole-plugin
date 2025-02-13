package com.github.bobi.aemgroovyconsoleplugin.utils

import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileWrapper
import java.util.*

fun isInternal(): Boolean {
    return java.lang.Boolean.getBoolean(ApplicationManagerEx.IS_INTERNAL_PROPERTY)
}

fun openSaveFileDialog(
    project: Project,
    extension: String,
    dialogTitle: String,
    filePrefix: String = "groovy-console"
): VirtualFileWrapper? {
    var outputDir = project.guessProjectDir()
    if (outputDir == null || !outputDir.exists()) {
        outputDir = VfsUtil.getUserHomeDir()
    }

    val saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(
        FileSaverDescriptor(dialogTitle, "", extension), project
    )

    val fileName =
        "${filePrefix}-%1\$tF-%1\$tH%1\$tM%1\$tS".let { if (SystemInfo.isMac) "$it.$extension" else it }
            .let { String.format(Locale.US, it, Calendar.getInstance()) }

    return saveFileDialog.save(outputDir, fileName)
}
