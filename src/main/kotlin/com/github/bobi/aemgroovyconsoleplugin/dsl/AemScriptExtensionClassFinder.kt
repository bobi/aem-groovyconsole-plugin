package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.icfolson.aem.groovy.extension.builders.NodeBuilder
import com.icfolson.aem.groovy.extension.builders.PageBuilder
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.NonClasspathDirectoriesScope
import org.cid15.aem.groovy.console.table.Table

/**
 * User: Andrey Bardashevsky
 * Date/Time: 01.08.2022 19:24
 *
 */
class AemScriptExtensionClassFinder(project: Project) : NonClasspathClassFinder(project, "groovy") {

    override fun calcClassRoots(): List<VirtualFile> = roots

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val packageName = StringUtil.getPackageName(qualifiedName)

        return if (supportedPackages.contains(packageName)) {
            super.findClass(qualifiedName, scope.uniteWith(searchScope))
        } else {
            null
        }
    }

    companion object {
        private val thirdPartyClasses = listOf(
            PageBuilder::class.java,
            NodeBuilder::class.java,
            Table::class.java,
        )

        private val supportedPackages = thirdPartyClasses.mapTo(HashSet<String>()) { it.`package`.name }
            .also {
                it.add("specs")
            }

        private val jarForClasses = listOf(
            *thirdPartyClasses.toTypedArray(),
            AemScriptExtensionClassFinder::class.java
        )

        private val roots = buildClassesRoots()

        val searchScope = NonClasspathDirectoriesScope.compose(roots)

        private fun buildClassesRoots(): List<VirtualFile> {
            val isInternal = java.lang.Boolean.getBoolean(ApplicationManagerEx.IS_INTERNAL_PROPERTY)

            return jarForClasses.mapNotNullTo(LinkedHashSet()) { clazz ->
                val jarForClass = PathManager.getJarForClass(clazz)

                var classRoot: VirtualFile? = null

                if (jarForClass != null) {
                    val virtualFile = VfsUtil.findFileByIoFile(jarForClass.toFile(), true)

                    if (virtualFile != null) {
                        classRoot = JarFileSystem.getInstance().getRootByLocal(virtualFile)
                    }
                }

                if (isInternal) {
                    // refresh changes during development
                    classRoot?.refresh(true, true)
                }

                return@mapNotNullTo classRoot
            }.toList()
        }
    }
}