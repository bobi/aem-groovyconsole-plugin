package com.github.bobi.aemgroovyconsoleplugin.dsl.builders

import be.orbinson.aem.groovy.console.builders.NodeBuilder

private val FQN: String = NodeBuilder::class.java.name

private val ORIGIN_INFO: String = "via ${AemNodeBuilderContributor::class.java.name}"

class AemNodeBuilderContributor : BuilderMethodsContributor() {
    override fun getParentClassName(): String = FQN

    override fun getFqnClassName(): String = FQN

    override fun getOriginInfo(): String = ORIGIN_INFO
}
