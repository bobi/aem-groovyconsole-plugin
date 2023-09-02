package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiElement
import groovy.lang.Closure
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames

/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.09.2022 00:01
 */

private val log: Logger = Logger.getInstance("#com.github.bobi.aemgroovyconsoleplugin.dsl")

sealed class Descriptor

data class TypeDescriptor(
    val fqn: String,
    val genericType: String? = null
) : Descriptor()

data class VariableDescriptor(
    val name: String,
    val type: TypeDescriptor,
    val doc: String?,
) : Descriptor()

data class GenericMethodDescriptor(
    val name: String,
    val genericTypes: List<String>,
    val parameters: List<VariableDescriptor>,
    val returnType: TypeDescriptor,
    val throws: List<String>,
    val containingClass: String?,
    val isStatic: Boolean,
    val bindsTo: PsiElement?,
    val doc: String?,
    val docUrl: String?,
) : Descriptor()

fun parseVariable(args: Map<*, *>): VariableDescriptor {
    return VariableDescriptor(
        name = args["name"].toString(),
        type = TypeDescriptor(stringifyType(args["type"])),
        doc = args["doc"] as? String,
    )
}

fun parseGenericMethod(args: Map<*, *>): GenericMethodDescriptor {
    return GenericMethodDescriptor(
        name = args["name"].toString(),
        genericTypes = genericTypes(args),
        parameters = params(args),
        returnType = parseType(args["type"]),
        containingClass = args["containingClass"] as? String,
        throws = throws(args),
        isStatic = args["isStatic"] == true,
        bindsTo = args["bindsTo"] as? PsiElement,
        doc = args["doc"] as? String,
        docUrl = args["docUrl"] as? String,
    )
}

private fun params(args: Map<*, *>): List<VariableDescriptor> {
    val params = args["params"] as? Map<*, *> ?: return emptyList()
    val result = ArrayList<VariableDescriptor>()
    var first = true
    for ((key, value) in params.entries) {
        if (!first || value !is List<*>) {
            result.add(VariableDescriptor(name = key.toString(), type = parseType(value), doc = null))
        }
        first = false
    }
    return result.toList()
}

private fun throws(args: Map<*, *>): List<String> {
    val throws = args["throws"]
    return when {
        throws is List<*> -> throws.map(::stringifyType).toList()
        throws != null -> listOf(stringifyType(throws))
        else -> emptyList()
    }
}

private fun genericTypes(args: Map<*, *>): List<String> {
    return when (val genericTypes = args["genericTypes"]) {
        is List<*> -> genericTypes.filterIsInstance<String>().toList()
        is String -> listOf(genericTypes)
        else -> emptyList()
    }
}

private fun parseType(declaredType: Any?): TypeDescriptor {
    val stringType = stringifyType(declaredType, true)

    val genericStart = stringType.indexOf('<')
    val genericEnd = stringType.lastIndexOf('>')

    return if (genericStart > 0 && genericEnd > 0) {
        val fqn = stringType.substring(0, genericStart)
        val genericType = stringType.substring(genericStart + 1, genericEnd)

        TypeDescriptor(fqn = fqn, genericType = genericType)
    } else {
        TypeDescriptor(fqn = stringType)
    }
}

private fun stringifyType(type: Any?, allowGenerics: Boolean = false): String {
    return when (type) {
        null -> CommonClassNames.JAVA_LANG_OBJECT
        is Closure<*> -> GroovyCommonClassNames.GROOVY_LANG_CLOSURE
        is Map<*, *> -> CommonClassNames.JAVA_UTIL_MAP
        is Class<*> -> type.name
        else -> {
            val s = type.toString()
            log.assertTrue(!s.startsWith("? extends"), s)
            log.assertTrue(!s.contains("?extends"), s)
            log.assertTrue(!s.contains("<null."), s)
            log.assertTrue(!s.startsWith("null."), s)
            if (!allowGenerics) log.assertTrue(!(s.contains(",") && !s.contains("<")), s)
            s
        }
    }
}
