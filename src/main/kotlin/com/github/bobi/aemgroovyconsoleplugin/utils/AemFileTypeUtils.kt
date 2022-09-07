package com.github.bobi.aemgroovyconsoleplugin.utils

import com.github.bobi.aemgroovyconsoleplugin.lang.AemConsoleLanguageFileType
import com.github.bobi.aemgroovyconsoleplugin.lang.AemConsoleScriptType
import com.github.bobi.aemgroovyconsoleplugin.services.RootFoldersService
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.GroovyEnabledFileType
import org.jetbrains.plugins.groovy.GroovyLanguage

/**
 * User: Andrey Bardashevsky
 * Date/Time: 01.08.2022 19:43
 */
@Suppress("unused")
object AemFileTypeUtils {

    private fun VirtualFile.isAemFileType(project: Project): Boolean {
        val fileType = this.fileType

        return if (fileType is AemConsoleLanguageFileType) {
            true
        } else {
            val isGroovyFileType = fileType is GroovyEnabledFileType
                    || (fileType is LanguageFileType && fileType.language === GroovyLanguage)

            isGroovyFileType && RootFoldersService.getInstance(project).isInsideAnyRoot(path)
        }
    }

    fun VirtualFile.isAemFile(project: Project): Boolean {
        return extension == AemConsoleScriptType.EXTENSION || isAemFileType(project)
    }

    fun PsiElement.isAemFile(): Boolean {
        return this.containingFile.isAemFile()
    }

    fun PsiFile.isAemFile(): Boolean {
        var virtualFile = this.virtualFile

        if (virtualFile == null) {
            virtualFile = this.originalFile.virtualFile
        }

        if (virtualFile == null) {
            virtualFile = this.viewProvider.virtualFile
        }

        return virtualFile.isAemFile(this.project)
    }
}