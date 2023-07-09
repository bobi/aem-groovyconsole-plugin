package com.github.bobi.aemgroovyconsoleplugin.services.model

/**
 * User: Andrey Bardashevsky
 * Date/Time: 08.07.2023 18:32
 */
data class AemDevToken(var ok: Boolean, var statusCode: Int, var accessToken: String)

data class AemCertTechnicalAccount(
    var clientId: String,
    var clientSecret: String
)

data class AemCertIntegration(
    var imsEndpoint: String,
    var metascopes: String,
    var technicalAccount: AemCertTechnicalAccount,
    var email: String,
    var id: String,
    var org: String,
    var privateKey: String,
    var publicKey: String,
    var certificateExpirationDate: String,
)

data class AemCertificateToken(
    var ok: Boolean,
    var statusCode: Int,
    var integration: AemCertIntegration
)

data class AccessToken(
    val token_type: String,
    val access_token: String,
    val expires_in: Long,
)
