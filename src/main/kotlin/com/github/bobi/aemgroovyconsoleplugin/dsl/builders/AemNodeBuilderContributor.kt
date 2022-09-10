package com.github.bobi.aemgroovyconsoleplugin.dsl.builders

import com.icfolson.aem.groovy.extension.builders.NodeBuilder

class AemNodeBuilderContributor : BuilderMethodsContributor() {
    override fun getParentClassName(): String = FQN

    override fun getFqnClassName(): String = FQN

    override fun getOriginInfo(): String = ORIGIN_INFO

    companion object {
        private val FQN = NodeBuilder::class.java.name

        private val ORIGIN_INFO: String = "via ${this::class.java.name}"
    }
}