package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.checkName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods

/**
 * User: Andrey Bardashevsky
 * Date/Time: 05.08.2022 14:18
 */
class GenericMethodsContributor : NonCodeMembersContributor() {

    private val groovyScriptPattern = GroovyPatterns.groovyScript()

    private val typeClassPattern = PsiJavaPatterns.psiType()
        .classType(
            PsiJavaPatterns.psiClass()
                .and(StandardPatterns.instanceOf(SyntheticElement::class.java))
        )

    private val methods = listOf(
        MethodDescriptor(
            name = "getService",
            type = TypeDescriptor(generic = true),
            parameters = listOf(
                ParameterDescriptor(
                    name = "type",
                    type = TypeDescriptor(fqn = CommonClassNames.JAVA_LANG_CLASS, generic = true)
                )
            ),
            doc = "Get the OSGi service instance for the given type"
        ),
        MethodDescriptor(
            name = "getServices",
            type = TypeDescriptor(CommonClassNames.JAVA_UTIL_LIST, generic = true),
            parameters = listOf(
                ParameterDescriptor(
                    name = "type",
                    type = TypeDescriptor(fqn = CommonClassNames.JAVA_LANG_CLASS, generic = true)
                ),
                ParameterDescriptor(name = "filter", type = TypeDescriptor(fqn = CommonClassNames.JAVA_LANG_STRING))
            ),
            doc = "Get an instance of a Sling Model class for the Resource at the given path"
        ),
        MethodDescriptor(
            name = "getModel",
            type = TypeDescriptor(generic = true),
            parameters = listOf(
                ParameterDescriptor(name = "path", type = TypeDescriptor(fqn = CommonClassNames.JAVA_LANG_STRING)),
                ParameterDescriptor(
                    name = "type",
                    type = TypeDescriptor(fqn = CommonClassNames.JAVA_LANG_CLASS, generic = true)
                )
            ),
            doc = "Get OSGi services for the given type and filter expression"
        )
    )

    override fun processDynamicElements(
        qualifierType: PsiType,
        processor: PsiScopeProcessor,
        place: PsiElement,
        state: ResolveState
    ) {
        if (!groovyScriptPattern.accepts(place.containingFile)) return
        if (!typeClassPattern.accepts(qualifierType)) return

        if (!processor.shouldProcessMethods()) return
        if (!place.isAemFile()) return

        methods.forEach {
            if (processor.checkName(it.name, state)) {
                val method = createGenericMethod(it, place)

                if (!ResolveUtil.processElement(processor, method, state)) return
            }
        }
    }

    private fun createGenericMethod(methodDescriptor: MethodDescriptor, place: PsiElement): GrLightMethodBuilder {
        val method = GrLightMethodBuilder(place.manager, methodDescriptor.name)

        method.originInfo = ORIGIN_INFO
        method.addModifier(PsiModifier.PUBLIC)
        method.navigationElement = place

        if (!methodDescriptor.doc.isNullOrBlank()) {
            method.putUserData(NonCodeMembersHolder.DOCUMENTATION, methodDescriptor.doc)
        }

        val genericType: PsiClassType = TypesUtil.createType(method.addTypeParameter("T"), place)
        methodDescriptor.parameters.forEach {
            method.addParameter(it.name, chooseType(it.type, place, genericType))
        }

        method.returnType = chooseType(methodDescriptor.type, place, genericType)

        return method
    }

    private fun chooseType(typeDescriptor: TypeDescriptor, place: PsiElement, genericType: PsiClassType): PsiClassType {
        if (typeDescriptor.fqn == null) {
            if (typeDescriptor.generic) {
                return genericType
            }
        } else {
            return if (typeDescriptor.generic) {
                TypesUtil.createGenericType(typeDescriptor.fqn, place, genericType)
            } else {
                TypesUtil.createType(typeDescriptor.fqn, place)
            }
        }

        throw IllegalArgumentException("Can't detect type")
    }

    companion object {
        private const val ORIGIN_INFO = "via AemGenericMethodsBuilder"
    }

    data class MethodDescriptor(
        val name: String,
        val type: TypeDescriptor,
        val parameters: List<ParameterDescriptor> = listOf(),
        val doc: String? = null
    )

    data class ParameterDescriptor(
        val name: String,
        val type: TypeDescriptor
    )

    data class TypeDescriptor(
        val fqn: String? = null,
        val generic: Boolean = false
    )
}