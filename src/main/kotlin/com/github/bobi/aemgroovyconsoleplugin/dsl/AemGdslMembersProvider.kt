package com.github.bobi.aemgroovyconsoleplugin.dsl

import com.github.bobi.aemgroovyconsoleplugin.utils.AemFileTypeUtils.isAemFile
import org.jetbrains.plugins.groovy.dsl.GdslMembersHolderConsumer
import org.jetbrains.plugins.groovy.dsl.dsltop.GdslMembersProvider

/**
 * User: Andrey Bardashevsky
 * Date/Time: 06.09.2022 03:10
 */
class AemGdslMembersProvider : GdslMembersProvider {
    fun isAemFile(consumer: GdslMembersHolderConsumer): Boolean {
        return consumer.place.isAemFile()
    }
}