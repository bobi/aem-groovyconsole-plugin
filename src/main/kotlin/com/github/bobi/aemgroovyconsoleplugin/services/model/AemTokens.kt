package com.github.bobi.aemgroovyconsoleplugin.services.model

import com.google.gson.annotations.SerializedName

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
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
)
