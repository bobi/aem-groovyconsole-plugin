package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer
import org.jetbrains.plugins.groovy.lang.psi.util.isCompileStatic

class AemClosureParameterEnhancer : AbstractClosureParameterEnhancer() {

    private val closures = listOf(
        ClosureParameterDescriptor("com.day.cq.wcm.api.Page", "recurse"),
        ClosureParameterDescriptor("javax.jcr.Node", "recurse"),
        ClosureParameterDescriptor("javax.jcr.Binary", "withBinary"),
    )

    override fun getClosureParameterType(expression: GrFunctionalExpression, index: Int): PsiType? {
        if (isCompileStatic(expression)) {
            return null
        }

        var parent = expression.parent
        if (parent is GrArgumentList) parent = parent.getParent()
        if (parent !is GrMethodCall) {
            return null
        }

        if (!expression.isAemFile()) {
            return null
        }

        val invokedExpression = parent.invokedExpression as? GrReferenceExpression ?: return null

        val methodName = invokedExpression.referenceName ?: return null

        val qualifier = invokedExpression.qualifierExpression ?: return null

        qualifier.type ?: return null

        val psiClass = PsiTypesUtil.getPsiClass(qualifier.type) ?: return null

        val parameterType =
            closures.find { it.type == psiClass.qualifiedName && it.method == methodName }?.parameterType

        if (parameterType != null) {
            return JavaPsiFacade.getElementFactory(expression.project)
                .createTypeFromText(parameterType, expression)
        }

        return null
    }

    private data class ClosureParameterDescriptor(
        val type: String,
        val method: String,
        val parameterType: String = type
    )
}