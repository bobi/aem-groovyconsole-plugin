package com.github.bobi.aemgroovyconsoleplugin.utils

import com.github.bobi.aemgroovyconsoleplugin.lang.AemConsoleScriptType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * User: Andrey Bardashevsky
 * Date/Time: 01.08.2022 19:43
 */
@Suppress("unused")
object AemFileTypeUtils {

    fun VirtualFile.isAemFile(): Boolean {
        return this.extension == AemConsoleScriptType.EXTENSION
    }

    fun PsiElement.isAemFile(): Boolean {
        return this.containingFile.isAemFile()
    }

    fun PsiFile.isAemFile(): Boolean {
        var virtualFile = this.virtualFile

        if (virtualFile == null) {
            virtualFile = this.viewProvider.virtualFile
        }

        return virtualFile.isAemFile()
    }
}