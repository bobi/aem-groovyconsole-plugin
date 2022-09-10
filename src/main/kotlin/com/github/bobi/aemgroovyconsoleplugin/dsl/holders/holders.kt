package com.github.bobi.aemgroovyconsoleplugin.dsl.holders

import com.github.bobi.aemgroovyconsoleplugin.dsl.Descriptor
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.plugins.groovy.dsl.ClosureDescriptor
import org.jetbrains.plugins.groovy.dsl.GroovyClassDescriptor
import org.jetbrains.plugins.groovy.dsl.holders.CustomMembersHolder
import java.util.function.Consumer

/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.09.2022 23:35
 */
open class CustomMembersHolderAdapter : CustomMembersHolder {
    override fun processMembers(
        descriptor: GroovyClassDescriptor,
        processor: PsiScopeProcessor,
        state: ResolveState
    ): Boolean {
        return true
    }

    override fun consumeClosureDescriptors(
        descriptor: GroovyClassDescriptor,
        consumer: Consumer<in ClosureDescriptor>
    ) = Unit
}

fun cacheableMembersHolder(
    memberDescriptor: Descriptor,
    predicate: (groovyClassDescriptor: GroovyClassDescriptor) -> Boolean = { true },
    holder: (groovyClassDescriptor: GroovyClassDescriptor) -> CustomMembersHolder
): CustomMembersHolder {
    return object : CustomMembersHolderAdapter() {
        override fun processMembers(
            descriptor: GroovyClassDescriptor,
            processor: PsiScopeProcessor,
            state: ResolveState
        ): Boolean {
            return when {
                predicate(descriptor) -> {
                    CachedValuesManager.getCachedValue(descriptor.justGetPlace()) {
                        CachedValueProvider.Result.create(
                            CollectionFactory.createConcurrentSoftMap<Descriptor, CustomMembersHolder>(),
                            PsiModificationTracker.MODIFICATION_COUNT
                        )
                    }.computeIfAbsent(memberDescriptor) { holder(descriptor) }
                        .processMembers(descriptor, processor, state)
                }

                else -> true
            }
        }
    }
}


