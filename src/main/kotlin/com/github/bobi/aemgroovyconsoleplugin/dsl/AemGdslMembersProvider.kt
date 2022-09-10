package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.github.bobi.aemgroovyconsoleplugin.dsl.holders.FieldHolder
import com.github.bobi.aemgroovyconsoleplugin.dsl.holders.GenericMethodHolder
import com.github.bobi.aemgroovyconsoleplugin.dsl.holders.cacheableMembersHolder
import org.jetbrains.plugins.groovy.dsl.GdslMembersHolderConsumer
import org.jetbrains.plugins.groovy.dsl.dsltop.GdslMembersProvider
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass

/**
 * User: Andrey Bardashevsky
 * Date/Time: 06.09.2022 03:10
 */
@Suppress("unused")
class AemGdslMembersProvider : GdslMembersProvider {

    fun field(args: Map<*, *>, consumer: GdslMembersHolderConsumer) {
        val fieldDescriptor = parseVariable(args)

        consumer.addMemberHolder(
            cacheableMembersHolder(
                fieldDescriptor,
                { it.justGetPsiClass() is GroovyScriptClass },
                { FieldHolder(fieldDescriptor, it.justGetPsiClass() as GroovyScriptClass, it.justGetPlaceFile()) }
            )
        )
    }

    fun genericMethod(args: Map<*, *>, consumer: GdslMembersHolderConsumer) {
        val methodDescriptor = parseGenericMethod(args)

        consumer.addMemberHolder(
            cacheableMembersHolder(methodDescriptor) {
                GenericMethodHolder(methodDescriptor, it.justGetPlaceFile())
            }
        )
    }
}