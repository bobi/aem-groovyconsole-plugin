package com.github.bobi.aemgroovyconsoleplugin.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import icons.JetgroovyIcons
import org.jetbrains.plugins.groovy.GroovyLanguage
import javax.swing.Icon

object AemConsoleLanguageFileType : LanguageFileType(GroovyLanguage, true) {

    override fun getIcon(): Icon = JetgroovyIcons.Groovy.GroovyFile
    override fun getName(): String = "AemConsole"
    override fun getDescription(): String = "Groovy AEM Script"
    override fun getDisplayName(): String = "Groovy AEM"
    override fun getDefaultExtension(): String = AemConsoleScriptType.EXTENSION

}
