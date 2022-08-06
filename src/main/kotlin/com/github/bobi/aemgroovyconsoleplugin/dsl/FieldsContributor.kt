package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.groovy.intentions.style.inference.resolve
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.checkName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessProperties

/**
 * User: Andrey Bardashevsky
 * Date/Time: 05.08.2022 14:18
 */
class FieldsContributor : NonCodeMembersContributor() {

    private val groovyScriptPattern = GroovyPatterns.groovyScript()

    private val typeClassPattern = PsiJavaPatterns.psiType()
        .classType(
            PsiJavaPatterns.psiClass()
                .and(StandardPatterns.instanceOf(SyntheticElement::class.java))
        )

    private val fields = listOf(
        FieldDescriptor("log", "org.slf4j.Logger"),
        FieldDescriptor("session", "javax.jcr.Session"),
        FieldDescriptor("pageManager", "com.day.cq.wcm.api.PageManager"),
        FieldDescriptor("resourceResolver", "org.apache.sling.api.resource.ResourceResolver"),
        FieldDescriptor("queryBuilder", "com.day.cq.search.QueryBuilder"),
        FieldDescriptor("nodeBuilder", "com.icfolson.aem.groovy.extension.builders.NodeBuilder"),
        FieldDescriptor("pageBuilder", "com.icfolson.aem.groovy.extension.builders.PageBuilder"),
        FieldDescriptor("bundleContext", "org.osgi.framework.BundleContext"),
        FieldDescriptor("out", "java.io.PrintStream"),
        FieldDescriptor("slingRequest", "org.apache.sling.api.SlingHttpServletRequest"),
        FieldDescriptor("slingResponse", "org.apache.sling.api.SlingHttpServletResponse"),
    )

    override fun processDynamicElements(
        qualifierType: PsiType,
        processor: PsiScopeProcessor,
        place: PsiElement,
        state: ResolveState
    ) {
        if (!groovyScriptPattern.accepts(place.containingFile)) return
        if (!typeClassPattern.accepts(qualifierType)) return

        if (!processor.shouldProcessProperties()) return
        if (!place.isAemFile()) return

        val clazz = qualifierType.resolve() ?: return
        if (clazz !is GroovyScriptClass) return

        fields.forEach {
            if (processor.checkName(it.name, state)) {
                val field = createField(it, clazz, place)

                if (!ResolveUtil.processElement(processor, field, state)) return
            }
        }
    }

    private fun createField(fieldDescriptor: FieldDescriptor, clazz: GroovyScriptClass, place: PsiElement): PsiField {
        val field = GrLightField(fieldDescriptor.name, fieldDescriptor.type, place)

        field.originInfo = ORIGIN_INFO

        return GrScriptField(field, clazz)
    }

    companion object {
        private const val ORIGIN_INFO = "via AemFieldsBuilder"
    }

    data class FieldDescriptor(
        val name: String,
        val type: String
    )
}