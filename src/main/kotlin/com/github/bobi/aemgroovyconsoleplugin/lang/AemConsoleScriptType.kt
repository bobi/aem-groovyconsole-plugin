package com.github.bobi.aemgroovyconsoleplugin.lang

import com.github.bobi.aemgroovyconsoleplugin.dsl.AemScriptExtensionClassFinder
import com.intellij.psi.search.GlobalSearchScope
import icons.JetgroovyIcons
import org.jetbrains.plugins.groovy.extensions.GroovyRunnableScriptType
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import javax.swing.Icon

/**
 * User: Andrey Bardashevsky
 * Date/Time: 03.08.2022 02:43
 */
class AemConsoleScriptType : GroovyRunnableScriptType(EXTENSION) {

    override fun getScriptIcon(): Icon = JetgroovyIcons.Groovy.GroovyFile

    override fun patchResolveScope(file: GroovyFile, baseScope: GlobalSearchScope): GlobalSearchScope {
        return baseScope.uniteWith(AemScriptExtensionClassFinder.searchScope)
            .uniteWith(GlobalSearchScope.allScope(file.project))
    }

    companion object {
        const val EXTENSION = "aemconsole"

        const val TEMPLATE = "AemConsole Script.aemconsole"

        @JvmStatic
        val instance = AemConsoleScriptType()
    }
}