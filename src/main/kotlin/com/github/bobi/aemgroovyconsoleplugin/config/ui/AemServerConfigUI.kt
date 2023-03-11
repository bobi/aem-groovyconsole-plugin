package com.github.bobi.aemgroovyconsoleplugin.config.ui

import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.intellij.credentialStore.Credentials

/**
 * User: Andrey Bardashevsky
 * Date/Time: 29.07.2022 23:26
 */
data class AemServerConfigUI(
    var id: Long = newId(),
    var name: String = "",
    var url: String = "",
    var user: String = "",
    var password: String = "",
    var distributedExecution: Boolean = false
) {
    fun duplicate(): AemServerConfigUI = copy(id = newId())

    fun toAemServerConfig(): AemServerConfig {
        return AemServerConfig(
            id = this.id,
            name = this.name,
            url = this.url,
            distributedExecution = this.distributedExecution
        )
    }

    companion object {
        private fun newId(): Long = System.currentTimeMillis()

        fun fromAemServerConfig(config: AemServerConfig, credentials: Credentials?): AemServerConfigUI {
            return AemServerConfigUI(
                id = config.id,
                name = config.name,
                url = config.url,
                user = credentials?.userName.orEmpty(),
                password = credentials?.getPasswordAsString().orEmpty(),
                distributedExecution = config.distributedExecution
            )
        }
    }
}