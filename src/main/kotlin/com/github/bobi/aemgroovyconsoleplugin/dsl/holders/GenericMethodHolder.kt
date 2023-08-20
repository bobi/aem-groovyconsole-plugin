package com.github.bobi.aemgroovyconsoleplugin.dsl.holders

import com.github.bobi.aemgroovyconsoleplugin.dsl.GenericMethodDescriptor
import com.github.bobi.aemgroovyconsoleplugin.dsl.TypeDescriptor
import com.intellij.psi.*
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.groovy.dsl.GroovyClassDescriptor
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.checkName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessMethods

/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.09.2022 00:48
 */
class GenericMethodHolder(descriptor: GenericMethodDescriptor, psiFile: PsiFile) :
    CustomMembersHolderAdapter() {

    private val method = createGenericMethod(descriptor, psiFile)

    override fun processMembers(
        descriptor: GroovyClassDescriptor,
        processor: PsiScopeProcessor,
        state: ResolveState
    ): Boolean {
        return !(processor.shouldProcessMethods()
                && processor.checkName(this.method.name, state)
                && !ResolveUtil.processElement(processor, method, state))
    }

    private fun createGenericMethod(descriptor: GenericMethodDescriptor, place: PsiElement): GrLightMethodBuilder {
        val psiMethod = GrLightMethodBuilder(place.manager, descriptor.name)

        psiMethod.originInfo = ORIGIN_INFO
        psiMethod.addModifier(PsiModifier.PUBLIC)

        if (descriptor.isStatic) {
            psiMethod.addModifier(PsiModifier.STATIC)
        }

        val bindsTo = descriptor.bindsTo
        if (bindsTo != null) {
            psiMethod.navigationElement = bindsTo
        }

        val toThrow = descriptor.throws
        for (o in toThrow) {
            psiMethod.addException(TypesUtil.createType(o, place))
        }

        val qName = descriptor.containingClass
        if (qName != null) {
            val foundClass = JavaPsiFacade.getInstance(place.project).findClass(qName, place.resolveScope)

            if (foundClass != null) {
                psiMethod.containingClass = foundClass
            }
        }

        if (!descriptor.doc.isNullOrBlank()) {
            psiMethod.putUserData(NonCodeMembersHolder.DOCUMENTATION, descriptor.doc)
        }

        val docUrl = descriptor.docUrl
        if (!docUrl.isNullOrBlank()) {
            psiMethod.putUserData(NonCodeMembersHolder.DOCUMENTATION_URL, docUrl)
        }

        val genericTypes = descriptor.genericTypes.associateWith {
            TypesUtil.createType(psiMethod.addTypeParameter(it), place)
        }

        descriptor.parameters.forEach {
            psiMethod.addParameter(it.name, chooseType(it.type, place, genericTypes))
        }

        psiMethod.returnType = chooseType(descriptor.returnType, place, genericTypes)

        return psiMethod
    }

    private fun chooseType(
        typeDescriptor: TypeDescriptor,
        place: PsiElement,
        genericTypes: Map<String, PsiClassType>
    ): PsiClassType {
        return genericTypes[typeDescriptor.fqn]
            ?: if (typeDescriptor.genericType != null) {
                TypesUtil.createGenericType(typeDescriptor.fqn, place, genericTypes[typeDescriptor.genericType])
            } else {
                TypesUtil.createType(typeDescriptor.fqn, place)
            }
    }

    companion object {
        private val ORIGIN_INFO = "via ${this::class.java.name}"
    }
}
