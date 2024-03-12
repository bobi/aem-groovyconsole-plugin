package com.github.bobi.aemgroovyconsoleplugin.services.http

import com.github.bobi.aemgroovyconsoleplugin.services.PasswordsService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.OutputTable
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemServerConfig
import com.github.bobi.aemgroovyconsoleplugin.services.model.AuthType
import com.github.bobi.aemgroovyconsoleplugin.utils.isInternal
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import io.ktor.utils.io.core.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.time.Duration
import java.util.*
import kotlin.io.use
import kotlin.text.toByteArray


/**
 * User: Andrey Bardashevsky
 * Date/Time: 02.08.2022 16:06
 */
@Service(Service.Level.PROJECT)
class GroovyConsoleHttpService(project: Project) {

    private val imsTokenProvider = AdobeIMSTokenProvider.getInstance(project)
    private val httpClient: OkHttpClient by lazy {
        val timeout = Duration.ofMinutes(10)

        val builder = OkHttpClient.Builder()

        if (isInternal()) {
            builder.addInterceptor(HttpLoggingInterceptor { println(it) }.setLevel(HttpLoggingInterceptor.Level.BODY))
        }

        builder.callTimeout(timeout)
        builder.writeTimeout(timeout)
        builder.connectTimeout(timeout)
        builder.readTimeout(timeout)

        return@lazy builder.build()
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
        val uri = config.url.toHttpUrl().newBuilder().encodedPath(action.path).build()

        val authorizationHeader = getAuthorizationHeader(config)

        val formBody = FormBody.Builder()
            .add("script", script.decodeToString())
            .build()

        val request: Request = Request.Builder()
            .url(uri)
            .addHeader(authorizationHeader.first, authorizationHeader.second)
            .post(formBody)
            .build()

        return httpClient.newCall(request).execute().use { response ->
            return@use handleResponse(response)
        }
    }

    private fun handleResponse(response: Response): GroovyConsoleOutput {
        val body = response.body

        if (response.isSuccessful && body != null) {
            val output = InputStreamReader(body.byteStream()).use {
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
                result = output.result,
                output = output.output,
                runningTime = output.runningTime,
                exceptionStackTrace = output.exceptionStackTrace,
                table = outputTable?.table
            )
        } else {
            throw IOException("${response.code} ${response.message}")
        }
    }

    private fun getAuthorizationHeader(config: AemServerHttpConfig): Pair<String, String> {
        val value = when (config.authType) {
            AuthType.BASIC -> "Basic " + String(
                Base64.getEncoder().encode("${config.credentials.user}:${config.credentials.password}".toByteArray())
            )

            else -> "Bearer " + imsTokenProvider.getAccessToken(config)
        }

        return Pair("Authorization", value)
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

