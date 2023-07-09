package com.github.bobi.aemgroovyconsoleplugin.services.model

data class AemServerConfig(
    var id: Long = -1,

    var name: String = "",

    var url: String = "",

    var distributedExecution: Boolean = false,

    var authType: AuthType = AuthType.BASIC,
)


enum class AuthType(private var title: String, var shortTitle: String = title) {
    BASIC("Basic"),
    DEV_TOKEN("AEMaaCS Local Token", "Token"),
    IMS_CERT("AEMaaCS Technical Account", "Token");

    override fun toString(): String {
        return this.title
    }
}
