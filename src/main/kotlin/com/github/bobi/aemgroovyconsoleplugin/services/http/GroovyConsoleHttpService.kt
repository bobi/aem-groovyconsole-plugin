package com.github.bobi.aemgroovyconsoleplugin.services.http

import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleTable
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.github.bobi.aemgroovyconsoleplugin.services.model.AuthType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.ktor.utils.io.core.*
import org.apache.hc.client5.http.classic.methods.HttpPost
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.utils.Base64
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpException
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.net.URIBuilder
import org.apache.hc.core5.util.Timeout
import java.io.InputStreamReader
import java.io.StringReader
import java.net.URI
import java.time.Duration
import kotlin.io.use
import kotlin.text.toByteArray

/**
 * User: Andrey Bardashevsky
 * Date/Time: 02.08.2022 16:06
 */
@Service(Service.Level.PROJECT)
class GroovyConsoleHttpService(project: Project) : Disposable {

    private val imsTokenProvider = AdobeIMSTokenProvider.getInstance(project)

    private val httpClient: CloseableHttpClient by lazy {
        val timeout = Timeout.of(Duration.ofMinutes(10))

        return@lazy HttpClients.custom()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                            .setConnectTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .build()
                    )
                    .build()
            )
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectionRequestTimeout(timeout)
                    .build()
            )
            .build()
    }

    private val gson: Gson = GsonBuilder().create()

    fun execute(httpConfig: AemServerHttpConfig, script: ByteArray) =
        execute(httpConfig, script, Action.EXECUTE)

    fun execute(config: AemServerConfig, script: ByteArray, action: Action) =
        execute(
            AemServerHttpConfig(
                id = config.id,
                url = config.url,
                authType = config.authType,
                credentials = getCredentials(config)
            ),
            script,
            action
        )

    private fun execute(config: AemServerHttpConfig, script: ByteArray, action: Action): GroovyConsoleOutput {
        val uri = URIBuilder().apply {
            val configHostUri = URI.create(config.url)

            scheme = configHostUri.scheme
            host = configHostUri.host
            port = configHostUri.port

            path = action.path
        }.build()

        val request = HttpPost(uri).apply {
            addHeader(getAuthorizationHeader(config))

            entity = MultipartEntityBuilder.create().addBinaryBody("script", script).build()
        }

        return httpClient.execute(request, ::handleResponse)
    }

    private fun handleResponse(response: ClassicHttpResponse): GroovyConsoleOutput {
        if (HttpStatus.SC_OK == response.code) {
            val output = InputStreamReader(response.entity.content).use {
                return@use gson.fromJson(it, Output::class.java)
            }

            var outputTable: OutputTable? = null

            if (!output.result.isNullOrBlank()) {
                try {
                    outputTable = StringReader(output.result).use {
                        return@use gson.fromJson(it, OutputTable::class.java)
                    }
                } catch (e: Throwable) {
                    thisLogger().info(e)
                }
            }

            return GroovyConsoleOutput(
                output = if (output.output.isNullOrBlank() && outputTable == null && output.result != null) {
                    output.result
                } else {
                    output?.output
                },
                runningTime = output.runningTime,
                exceptionStackTrace = output.exceptionStackTrace,
                table = outputTable?.table
            )
        } else {
            throw HttpException("${response.code} ${response.reasonPhrase}")
        }
    }

    private fun getAuthorizationHeader(config: AemServerHttpConfig): Header {
        val value = when (config.authType) {
            AuthType.BASIC -> "Basic " + String(Base64.encodeBase64("${config.credentials.user}:${config.credentials.password}".toByteArray()))
            else -> "Bearer " + imsTokenProvider.getAccessToken(config)
        }

        return BasicHeader("Authorization", value)
    }

    override fun dispose() {
        httpClient.close()
    }

    companion object {

        fun getInstance(project: Project): GroovyConsoleHttpService = project.service()

        private fun getCredentials(config: AemServerConfig): AemServerCredentials {
            val credentials = PasswordsService.getCredentials(config.id)

            val user = credentials?.userName.orEmpty()
            val password = credentials?.getPasswordAsString().orEmpty()

            require(user.isNotBlank() && password.isNotBlank()) { "Credentials is not found for server: ${config.id}" }

            return AemServerCredentials(user, password)
        }
    }

    private data class Output(
        val output: String? = null,
        val runningTime: String? = null,
        val exceptionStackTrace: String? = null,
        val result: String? = null
    )

    private data class OutputTable(val table: GroovyConsoleTable)

    enum class Action(internal val path: String) {
        EXECUTE("/bin/groovyconsole/post.json"),
        EXECUTE_DISTRIBUTED("/bin/groovyconsole/replicate"),
    }
}

class AemServerCredentials(val user: String, val password: String)

class AemServerHttpConfig(
    val id: Long? = null,
    val url: String,
    val authType: AuthType,
    val credentials: AemServerCredentials
)

