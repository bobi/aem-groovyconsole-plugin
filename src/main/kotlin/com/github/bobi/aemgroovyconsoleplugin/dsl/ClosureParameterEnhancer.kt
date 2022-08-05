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

private const val CLASS_AEM_PAGE = "com.day.cq.wcm.api.Page"
private const val CLASS_JCR_NODE = "javax.jcr.Node"
private const val CLASS_BINARY = "javax.jcr.Binary"

private const val METHOD_RECURSE = "recurse"
private const val METHOD_WITH_BINARY = "withBinary"

class ClosureParameterEnhancer : AbstractClosureParameterEnhancer() {

    override fun getClosureParameterType(expression: GrFunctionalExpression, index: Int): PsiType? {
        if (!expression.isAemFile()) {
            return null
        }

        if (isCompileStatic(expression)) {
            return null
        }

        var parent = expression.parent
        if (parent is GrArgumentList) parent = parent.getParent()
        if (parent !is GrMethodCall) {
            return null
        }

        val invokedExpression = parent.invokedExpression as? GrReferenceExpression ?: return null

        val methodName = invokedExpression.referenceName

        val qualifier = invokedExpression.qualifierExpression ?: return null

        qualifier.type ?: return null

        val psiClass = PsiTypesUtil.getPsiClass(qualifier.type) ?: return null

        if (psiClass.qualifiedName == CLASS_BINARY && methodName == METHOD_WITH_BINARY) {
            return JavaPsiFacade.getElementFactory(expression.project).createTypeFromText(psiClass.qualifiedName!!, expression)
        }

        if (psiClass.qualifiedName == CLASS_AEM_PAGE && methodName == METHOD_RECURSE) {
            return JavaPsiFacade.getElementFactory(expression.project).createTypeFromText(psiClass.qualifiedName!!, expression)
        }

        if (psiClass.qualifiedName == CLASS_JCR_NODE && methodName == METHOD_RECURSE) {
            return JavaPsiFacade.getElementFactory(expression.project).createTypeFromText(psiClass.qualifiedName!!, expression)
        }

        return null
    }
}