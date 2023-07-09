package com.github.bobi.aemgroovyconsoleplugin.services.http

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AccessToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemCertificateToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemDevToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AuthType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.io.entity.EntityUtils
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.net.URIBuilder
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.07.2023 14:07
 */
@Service(Service.Level.PROJECT)
class AdobeIMSTokenProvider : Disposable {
    private val gson: Gson = GsonBuilder().create()

    private val httpClient: CloseableHttpClient by lazy {
        return@lazy HttpClients.createDefault()
    }

    fun getAccessToken(config: AemServerHttpConfig): String {
        return when (config.authType) {
            AuthType.DEV_TOKEN -> parseDevToken(config.credentials.password).accessToken
            AuthType.IMS_CERT -> getImsToken(config)
            else -> throw Exception("Unsupported Authentication Type")
        }
    }

    private fun getImsToken(config: AemServerHttpConfig): String {
        if (config.id == null) {
            return fetchAccessToken(config).access_token
        } else {
            try {
                val token = PasswordsService.getAccessToken(config.id)
                if (token !== null) {
                    val accessToken = gson.fromJson(token, AccessToken::class.java)

                    val decodedToken = JWT.decode(accessToken.access_token)

                    if (decodedToken.expiresAtAsInstant.isAfter(Instant.now())) {
                        throw Exception("Token expired")
                    }

                    return accessToken.access_token
                } else {
                    throw Exception("Token not found")
                }
            } catch (e: Throwable) {
                val accessToken = fetchAccessToken(config)

                PasswordsService.setAccessToken(config.id, gson.toJson(accessToken))

                return accessToken.access_token
            }
        }
    }

    private fun fetchAccessToken(config: AemServerHttpConfig): AccessToken {
        val (_, _, integration) = parseCertToken(config.credentials.password)

        val jwtBuilder = JWT.create()
            .withIssuer(integration.org)
            .withSubject(integration.id)
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(8, ChronoUnit.HOURS))
            .withAudience("https://${integration.imsEndpoint}/c/${integration.technicalAccount.clientId}")

        integration.metascopes.split(",").forEach {
            jwtBuilder.withClaim("https://${integration.imsEndpoint}/s/${it}", true)
        }

        val jwtToken = jwtBuilder.sign(Algorithm.RSA256(readPrivateKey(integration.privateKey)))

        val uri = URIBuilder().apply {
            scheme = "https"
            host = integration.imsEndpoint
            path = "/ims/exchange/jwt"
        }.build()

        val request = HttpPost(uri).apply {
            entity = UrlEncodedFormEntity(
                listOf(
                    BasicNameValuePair("client_id", integration.technicalAccount.clientId),
                    BasicNameValuePair("client_secret", integration.technicalAccount.clientSecret),
                    BasicNameValuePair("jwt_token", jwtToken)
                )
            )
        }

        val token = httpClient.execute(request) { response ->
            if (HttpStatus.SC_OK == response.code) {
                val token = EntityUtils.toString(response.entity)

                return@execute gson.fromJson(token, AccessToken::class.java)
            } else {
                throw HttpException("${response.code} ${response.reasonPhrase}")
            }
        }

        return token
    }

    private fun parseDevToken(token: String): AemDevToken {
        return gson.fromJson(token, AemDevToken::class.java)
    }

    private fun parseCertToken(token: String): AemCertificateToken {
        return gson.fromJson(token, AemCertificateToken::class.java)
    }

    override fun dispose() {
        httpClient.close()
    }

    companion object {
        fun getInstance(project: Project): AdobeIMSTokenProvider = project.service()

        private fun readPrivateKey(key: String): RSAPrivateKey {
            val pemParser = PEMParser(StringReader(key))
            val converter = JcaPEMKeyConverter()
            val readObject = pemParser.readObject() as PEMKeyPair
            val kp = converter.getKeyPair(readObject)

            val kf = KeyFactory.getInstance("RSA")

            return kf.generatePrivate(PKCS8EncodedKeySpec(kp.private.encoded)) as RSAPrivateKey
        }

    }
}
