package com.github.bobi.aemgroovyconsoleplugin.dsl.builders

import be.orbinson.aem.groovy.console.builders.PageBuilder

class AemPageBuilderContributor : BuilderMethodsContributor() {
    override fun getParentClassName(): String = FQN

    override fun getFqnClassName(): String = FQN

    override fun getOriginInfo(): String = ORIGIN_INFO

    companion object {
        private val FQN = PageBuilder::class.java.name

        private val ORIGIN_INFO: String = "via ${this::class.java.name}"
    }
}