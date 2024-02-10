package com.github.bobi.aemgroovyconsoleplugin.services.http

import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.model.AccessToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemCertificateToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemDevToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AuthType
import com.github.bobi.aemgroovyconsoleplugin.utils.invokeAndWaitIfNeeded
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.jsonwebtoken.Jwts
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.IOException
import java.io.StringReader
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration


/**
 * User: Andrey Bardashevsky
 * Date/Time: 09.07.2023 14:07
 */
@Service(Service.Level.PROJECT)
class AdobeIMSTokenProvider {
    private val gson: Gson = GsonBuilder().create()

    private val httpClient: OkHttpClient by lazy {
        return@lazy OkHttpClient()
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
            return fetchAccessToken(config).accessToken
        } else {
            try {
                val token = invokeAndWaitIfNeeded { runReadAction { PasswordsService.getAccessToken(config.id) } }
                if (token !== null) {
                    val accessToken = verifyAccessToken(token)

                    return accessToken.accessToken
                } else {
                    throw Exception("Token not found")
                }
            } catch (e: Throwable) {
                val accessToken = fetchAccessToken(config)

                invokeAndWaitIfNeeded {
                    runWriteAction {
                        PasswordsService.setAccessToken(
                            config.id,
                            gson.toJson(accessToken)
                        )
                    }
                }

                return accessToken.accessToken
            }
        }
    }

    private fun verifyAccessToken(token: String): AccessToken {
        val accessToken = gson.fromJson(token, AccessToken::class.java)

        val splitToken = accessToken.accessToken.split(Regex("\\."))

        val payload = gson.fromJson(
            Base64.getDecoder().decode(splitToken[1]).decodeToString(),
            object : TypeToken<Map<String, *>>() {}
        )

        val createdAt = payload["created_at"]?.toString()?.toLong() ?: 0
        val expiresIn = payload["expires_in"]?.toString()?.toLong() ?: 0

        val expiresAt = Instant.ofEpochMilli(createdAt + expiresIn).minus(1.hours.toJavaDuration())

        if (expiresAt.isBefore(Instant.now())) {
            throw Exception("Expired token")
        }

        return accessToken
    }

    private fun fetchAccessToken(config: AemServerHttpConfig): AccessToken {
        val (_, _, integration) = parseCertToken(config.credentials.password)

        val jwtBuilder = Jwts.builder()
            .issuer(integration.org)
            .subject(integration.id)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plus(8, ChronoUnit.HOURS)))
            .audience().add("https://${integration.imsEndpoint}/c/${integration.technicalAccount.clientId}")
            .and()

        integration.metascopes.split(",").forEach {
            jwtBuilder.claim("https://${integration.imsEndpoint}/s/${it}", true)
        }

        val jwtToken = jwtBuilder.signWith(readPrivateKey(integration.privateKey), Jwts.SIG.RS256).compact()

        val uri = HttpUrl.Builder()
            .scheme("https")
            .host(integration.imsEndpoint)
            .encodedPath("/ims/exchange/jwt")
            .build()

        val formBody = FormBody.Builder()
            .add("client_id", integration.technicalAccount.clientId)
            .add("client_secret", integration.technicalAccount.clientSecret)
            .add("jwt_token", jwtToken)
            .build()

        val request: Request = Request.Builder()
            .url(uri)
            .post(formBody)
            .build()

        val token = httpClient.newCall(request).execute().use { response ->
            val body = response.body
            if (response.isSuccessful && body != null) {
                return@use gson.fromJson(body.string(), AccessToken::class.java)
            } else {
                throw IOException("${response.code} ${response.message}")
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
