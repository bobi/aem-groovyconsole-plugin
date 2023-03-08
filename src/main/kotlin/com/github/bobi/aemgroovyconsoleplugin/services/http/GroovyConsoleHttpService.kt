package com.github.bobi.aemgroovyconsoleplugin.services.http

import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleTable
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import org.apache.http.HttpException
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthScope
import org.apache.http.auth.Credentials
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URIUtils
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.io.InputStreamReader
import java.io.StringReader
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * User: Andrey Bardashevsky
 * Date/Time: 02.08.2022 16:06
 */
class GroovyConsoleHttpService : Disposable {
    private val httpClient: CloseableHttpClient = createHttpRequest()

    private val gson: Gson = GsonBuilder().create()

    private fun createHttpRequest(): CloseableHttpClient {
        val timeout = TimeUnit.MINUTES.toMillis(10).toInt()

        return HttpClientBuilder.create()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout)
                    .build()
            )
            .build()
    }

    fun execute(url: String, user: String, password: String, script: ByteArray, action: Action) =
        execute(createAemHttpConfig(url, user, password), script, action)

    fun execute(config: AemServerConfig, script: ByteArray, action: Action) = execute(createAemHttpConfig(config), script, action)

    private fun execute(config: AemServerHttpConfig, script: ByteArray, action: Action): GroovyConsoleOutput {
        val uri = URIBuilder().also {
            val configHostUri = URI.create(config.url)

            it.scheme = configHostUri.scheme
            it.host = configHostUri.host
            it.port = configHostUri.port

            it.path = action.path
        }.build()

        val httpHost = URIUtils.extractHost(uri)

        val context: HttpClientContext = HttpClientContext.create().also { context ->
            context.authCache = BasicAuthCache().also { cache ->
                cache.put(httpHost, BasicScheme())
            }
            context.credentialsProvider = BasicCredentialsProvider().also { credentialsProvider ->
                credentialsProvider.setCredentials(AuthScope(httpHost), config.credentials)
            }
        }

        val request = HttpPost(uri).also {
            it.entity = MultipartEntityBuilder.create().addBinaryBody("script", script).build()
        }

        return httpClient.execute(httpHost, request, ::handleResponse, context)
    }

    private fun handleResponse(response: HttpResponse): GroovyConsoleOutput {
        if (HttpStatus.SC_OK == response.statusLine.statusCode) {
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
            throw HttpException(response.statusLine.toString())
        }
    }

    override fun dispose() {
        httpClient.close()
    }

    companion object {

        fun getInstance(project: Project): GroovyConsoleHttpService = project.service()

        private fun getCredentials(config: AemServerConfig): Credentials {
            val credentials = PasswordsService.getCredentials(config.id)

            val user = credentials?.userName.orEmpty()
            val password = credentials?.getPasswordAsString().orEmpty()

            if (user.isBlank() || password.isBlank()) {
                throw IllegalArgumentException("Credentials is not found for server: ${config.id}")
            }

            return UsernamePasswordCredentials(user, password)
        }

        private fun createAemHttpConfig(cfg: AemServerConfig): AemServerHttpConfig {
            return AemServerHttpConfig(url = cfg.url, credentials = getCredentials(cfg))
        }

        private fun createAemHttpConfig(url: String, user: String, password: String): AemServerHttpConfig {
            return AemServerHttpConfig(url = url, credentials = UsernamePasswordCredentials(user, password))
        }
    }

    private data class Output(val output: String? = null, val runningTime: String? = null, val exceptionStackTrace: String? = null, val result: String? = null)

    private data class OutputTable(val table: GroovyConsoleTable)

    private class AemServerHttpConfig(val url: String, val credentials: Credentials)

    enum class Action(internal val path: String) {
        EXECUTE("/bin/groovyconsole/post.json"),
        EXECUTE_DISTRIBUTED("/bin/groovyconsole/replicate"),
    }
}