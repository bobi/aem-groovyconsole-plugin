package com.github.bobi.aemgroovyconsoleplugin.dsl.holders

import com.github.bobi.aemgroovyconsoleplugin.dsl.VariableDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.groovy.dsl.GroovyClassDescriptor
import org.jetbrains.plugins.groovy.dsl.holders.NonCodeMembersHolder
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil
import org.jetbrains.plugins.groovy.lang.resolve.checkName
import org.jetbrains.plugins.groovy.lang.resolve.shouldProcessFields

/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.09.2022 00:48
 */
class FieldHolder(descriptor: VariableDescriptor, clazz: GroovyScriptClass, psiFile: PsiFile) :
    CustomMembersHolderAdapter() {

    private val field = createField(descriptor, clazz, psiFile)

    override fun processMembers(
        descriptor: GroovyClassDescriptor,
        processor: PsiScopeProcessor,
        state: ResolveState
    ): Boolean {
        return !(processor.shouldProcessFields()
                && processor.checkName(this.field.name, state)
                && !ResolveUtil.processElement(processor, field, state))
    }

    private fun createField(descriptor: VariableDescriptor, clazz: GroovyScriptClass, place: PsiElement): PsiField {
        val psiField = GrLightField(descriptor.name, descriptor.type.fqn, place)

        psiField.originInfo = ORIGIN_INFO

        if (!descriptor.doc.isNullOrBlank()) {
            psiField.putUserData(NonCodeMembersHolder.DOCUMENTATION, descriptor.doc)
        }

        return GrScriptField(psiField, clazz)
    }

    companion object {
        private val ORIGIN_INFO = "via ${this::class.java.name}"
    }
}