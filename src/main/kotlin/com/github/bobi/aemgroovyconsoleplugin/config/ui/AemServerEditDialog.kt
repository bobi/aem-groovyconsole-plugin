package com.github.bobi.aemgroovyconsoleplugin.config.ui

import com.github.bobi.aemgroovyconsoleplugin.services.http.AemServerCredentials
import com.github.bobi.aemgroovyconsoleplugin.services.http.AemServerHttpConfig
import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemCertificateToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AemDevToken
import com.github.bobi.aemgroovyconsoleplugin.services.model.AuthType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.selectedValueIs
import com.intellij.util.ui.UIUtil
import org.apache.commons.lang3.exception.ExceptionUtils
import org.jetbrains.concurrency.AsyncPromise
import java.awt.event.ActionEvent
import javax.swing.*

private const val TOKEN_DESCRIPTION = "Paste JSON from Adobe AEM Developer Console"

private const val INVALID_JSON_MESSAGE = "Invalid json"

/**
 * User: Andrey Bardashevsky
 * Date/Time: 29.07.2022 15:45
 */
class AemServerEditDialog(private val project: Project, private val tableItem: AemServerConfigUI) :
    DialogWrapper(project) {

    private val httpService = GroovyConsoleHttpService.getInstance(project)

    private val gson: Gson = GsonBuilder().create()

    private val urlRegex = Regex(
        "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}(\\.[a-zA-Z0-9()]{1,6})?\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    private lateinit var urlField: JBTextField

    private lateinit var userField: JBTextField

    private lateinit var authTypeField: ComboBox<AuthType>

    private lateinit var passwordField: JPasswordField

    private lateinit var devTokenField: JTextArea

    private lateinit var imsCertField: JTextArea

    private lateinit var testResultField: JLabel

    private lateinit var distributedExecutionField: JBCheckBox

    private lateinit var testServerAction: Action

    init {
        title = "Aem Server"

        init()
    }

    @Suppress("kotlin:S3776")
    override fun createCenterPanel(): JComponent = panel {
        row("Server Name: ") {
            textField()
                .align(AlignX.FILL)
                .resizableColumn()
                .bindText({ tableItem.name }, { tableItem.name = it })
                .validationOnInput { validateNotEmpty(it.text) }
                .validationOnApply { validateNotEmpty(it.text) }
                .focused()
        }

        row("URL: ") {
            urlField = textField()
                .align(AlignX.FILL)
                .resizableColumn()
                .bindText({ tableItem.url }, { tableItem.url = it })
                .validationOnInput { validateUrl(it.text) }
                .validationOnApply { validateUrl(it.text) }
                .component
        }

        row("Authentication Type: ") {
            authTypeField = comboBox(AuthType.values().toList())
                .align(AlignX.FILL)
                .resizableColumn()
                .bindItem({ tableItem.authType }, { tableItem.authType = it!! })
                .onChanged { doValidateAll() }
                .component
        }

        row("Username: ") {
            userField = textField()
                .align(AlignX.FILL)
                .resizableColumn()
                .bindText(
                    { if (tableItem.authType == AuthType.BASIC) tableItem.user else "" },
                    { if (tableItem.authType == AuthType.BASIC) tableItem.user = it }
                )
                .validationOnInput { validateBasicAuthNotEmpty(authTypeField.item, it.text) }
                .validationOnApply { validateBasicAuthNotEmpty(authTypeField.item, it.text) }
                .enabledIf(authTypeField.selectedValueIs(AuthType.BASIC))
                .component
        }.visibleIf(authTypeField.selectedValueIs(AuthType.BASIC))

        row("Password: ") {
            passwordField = cell(JPasswordField())
                .columns(COLUMNS_SHORT)
                .align(AlignX.FILL)
                .resizableColumn()
                .bind(
                    JPasswordField::getPassword,
                    { field, value -> field.text = String(value) },
                    MutableProperty(
                        { if (tableItem.authType == AuthType.BASIC) tableItem.password.toCharArray() else "".toCharArray() },
                        { if (authTypeField.item == AuthType.BASIC) tableItem.password = String(it) }
                    )
                )
                .validationOnInput { validateBasicAuthNotEmpty(authTypeField.item, String(it.password)) }
                .validationOnApply { validateBasicAuthNotEmpty(authTypeField.item, String(it.password)) }
                .enabledIf(authTypeField.selectedValueIs(AuthType.BASIC))
                .component
        }.visibleIf(authTypeField.selectedValueIs(AuthType.BASIC))

        row("Token: ") {
            devTokenField = textArea()
                .rows(3)
                .align(AlignX.FILL)
                .resizableColumn()
                .bindText(
                    { if (tableItem.authType == AuthType.DEV_TOKEN) tableItem.password else "" },
                    {
                        if (authTypeField.item == AuthType.DEV_TOKEN) {
                            tableItem.user = authTypeField.item.shortTitle
                            tableItem.password = it
                        }
                    }
                )
                .validationOnInput { validateDevTokenValidJson(authTypeField.item, it.text) }
                .validationOnApply { validateDevTokenValidJson(authTypeField.item, it.text) }
                .enabledIf(authTypeField.selectedValueIs(AuthType.DEV_TOKEN))
                .component
            contextHelp(TOKEN_DESCRIPTION)
        }.visibleIf(authTypeField.selectedValueIs(AuthType.DEV_TOKEN))

        row("Certificate:") {
            imsCertField = textArea()
                .rows(3)
                .align(AlignX.FILL)
                .resizableColumn()
                .bindText(
                    { if (tableItem.authType == AuthType.IMS_CERT) tableItem.password else "" },
                    {
                        if (authTypeField.item == AuthType.IMS_CERT) {
                            tableItem.user = authTypeField.item.shortTitle
                            tableItem.password = it
                        }
                    }
                )
                .validationOnInput { validateImsCertValidJson(authTypeField.item, it.text) }
                .validationOnApply { validateImsCertValidJson(authTypeField.item, it.text) }
                .enabledIf(authTypeField.selectedValueIs(AuthType.IMS_CERT))
                .component
            contextHelp(TOKEN_DESCRIPTION)
        }.visibleIf(authTypeField.selectedValueIs(AuthType.IMS_CERT))

        row("") {
            distributedExecutionField = checkBox("Enable distributed execution")
                .align(AlignX.LEFT)
                .bindSelected({ tableItem.distributedExecution }, { tableItem.distributedExecution = it })
                .component
            contextHelp("OSGI 'distributedExecutionEnabled' property must be set to 'true'.")
        }

        row {
            testResultField = label("")
                .align(Align.FILL)
                .resizableColumn()
                .component
                .also {
                    it.isFocusable = false
                    it.isVisible = false
                }
        }.layout(RowLayout.INDEPENDENT).resizableRow()
    }.withPreferredWidth(500)

    override fun createDefaultActions() {
        super.createDefaultActions()

        testServerAction = object : DialogWrapperAction("Test") {
            override fun doAction(e: ActionEvent) {
                testConnectionToServer()
            }
        }

        okAction.addPropertyChangeListener {
            if ("enabled" == it.propertyName) {
                testServerAction.isEnabled = it.newValue as Boolean
            }
        }
    }

    override fun createActions(): Array<out Action> {
        return arrayOf(testServerAction, *super.createActions())
    }

    private fun testConnectionToServer() {
        val validationInfos = doValidateAll()

        updateErrorInfo(validationInfos)

        if (validationInfos.isEmpty()) {
            updateTestResult("")

            val promise = AsyncPromise<GroovyConsoleOutput>().also {
                it.onSuccess { out ->
                    if (out.output == "test") {
                        runInEdt { updateTestResult("Test success!") }
                    } else {
                        runInEdt { updateTestResult("Test fail. Output: ${out.output}", true) }
                    }
                }.onError { ex ->
                    runInEdt {
                        val rootCause = ExceptionUtils.getRootCause(ex)
                        val message = rootCause.localizedMessage ?: rootCause.javaClass.simpleName

                        updateTestResult("Test fail. Error: $message", true)
                    }
                }
            }

            val config = AemServerHttpConfig(
                url = urlField.text,
                authType = authTypeField.item,
                credentials = AemServerCredentials(
                    user = userField.text,
                    password = when (authTypeField.item) {
                        AuthType.BASIC -> String(passwordField.password)
                        AuthType.DEV_TOKEN -> devTokenField.text
                        AuthType.IMS_CERT -> imsCertField.text
                        else -> ""
                    }
                ),
            )
            runModalTask("Checking AEM Server Connection", project, false) {
                try {
                    promise.setResult(httpService.execute(config, "print('test')".toByteArray()))
                } catch (th: Throwable) {
                    thisLogger().info(th)
                    promise.setError(th)
                }
            }
        } else {
            val firstError = validationInfos.first()

            if (firstError.component?.isVisible == true) {
                IdeFocusManager.getInstance(null).requestFocus(firstError.component!!, true)
            }
        }
    }

    private fun updateTestResult(text: String, error: Boolean = false) {
        val preferredSize = testResultField.preferredSize
        val color = if (error) UIUtil.getErrorForeground() else UIUtil.getLabelSuccessForeground()
        val html = HtmlBuilder()
            .append(HtmlChunk.raw(text).wrapWith(HtmlChunk.font(ColorUtil.toHex(color))))
            .wrapWithHtmlBody().toString()

        testResultField.text = html

        testResultField.preferredSize = preferredSize
        testResultField.isVisible = text.isNotBlank()
    }

    private fun ValidationInfoBuilder.validateBasicAuthNotEmpty(authType: AuthType, value: String): ValidationInfo? {
        if (authType == AuthType.BASIC) {
            return validateNotEmpty(value)
        }

        return null
    }

    private fun ValidationInfoBuilder.validateDevTokenValidJson(authType: AuthType, value: String): ValidationInfo? {
        if (authType == AuthType.DEV_TOKEN) {
            val validationInfo = validateNotEmpty(value)

            if (validationInfo != null) {
                return validationInfo
            }

            try {
                val (ok, statusCode, accessToken) = gson.fromJson(value, AemDevToken::class.java)

                if (!ok || statusCode != 200 || accessToken.isBlank()) {
                    return error(INVALID_JSON_MESSAGE)
                }
            } catch (e: Throwable) {
                return error(INVALID_JSON_MESSAGE)
            }
        }

        return null
    }

    private fun ValidationInfoBuilder.validateImsCertValidJson(authType: AuthType, value: String): ValidationInfo? {
        if (authType == AuthType.IMS_CERT) {
            val validationInfo = validateNotEmpty(value)

            if (validationInfo != null) {
                return validationInfo
            }

            try {
                val (ok, statusCode, integration) = gson.fromJson(value, AemCertificateToken::class.java)

                if (!ok || statusCode != 200
                    || integration.imsEndpoint.isBlank()
                    || integration.org.isBlank()
                    || integration.id.isBlank()
                    || integration.technicalAccount.clientId.isBlank()
                    || integration.technicalAccount.clientSecret.isBlank()
                    || integration.metascopes.isBlank()
                    || integration.privateKey.isBlank()
                    || integration.publicKey.isBlank()
                ) {
                    return error(INVALID_JSON_MESSAGE)
                }
            } catch (e: Throwable) {
                return error(INVALID_JSON_MESSAGE)
            }
        }

        return null
    }

    private fun ValidationInfoBuilder.validateNotEmpty(value: String): ValidationInfo? {
        if (value.isEmpty()) {
            return error("Must not be empty")
        }

        return null
    }

    private fun ValidationInfoBuilder.validateUrl(value: String): ValidationInfo? {
        val validationInfo = validateNotEmpty(value)

        if (validationInfo != null) {
            return validationInfo
        }

        if (!urlRegex.matches(value)) {
            return error("Not a valid url")
        }

        return null
    }
}
