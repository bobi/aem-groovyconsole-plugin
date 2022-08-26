package com.github.bobi.aemgroovyconsoleplugin.config.ui

import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.PropertyBinding
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.panel
import com.intellij.ui.layout.withTextBinding
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPasswordField

/**
 * User: Andrey Bardashevsky
 * Date/Time: 29.07.2022 15:45
 */
class AemServerEditDialog(private val project: Project, private val tableItem: AemServerConfigUI) :
    DialogWrapper(project) {

    private val httpService = GroovyConsoleHttpService.getInstance(project)

    private val urlRegex = Regex(
        "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}(\\.[a-zA-Z0-9()]{1,6})?\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    private lateinit var urlField: JBTextField
    private lateinit var userField: JBTextField
    private lateinit var passwordField: JPasswordField
    private lateinit var testResultField: JLabel

    init {
        title = "Aem Server"

        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Server Name: ") {
            textField({ tableItem.name }, { tableItem.name = it })
                .withValidationOnInput { validateNotEmpty(it.text) }
                .withValidationOnApply { validateNotEmpty(it.text) }
                .focused()
        }

        row("URL: ") {
            urlField = textField({ tableItem.url }, { tableItem.url = it })
                .withValidationOnInput { validateUrl(it.text) }
                .withValidationOnApply { validateUrl(it.text) }
                .component
        }

        row("Username: ") {
            userField = textField({ tableItem.user }, { tableItem.user = it })
                .withValidationOnInput { validateNotEmpty(it.text) }
                .withValidationOnApply { validateNotEmpty(it.text) }
                .component
        }

        row("Password: ") {
            val binding = PropertyBinding({ tableItem.password }, { tableItem.password = it })

            passwordField = component(JPasswordField(binding.get(), 0)).withTextBinding(binding)
                .withValidationOnInput { validateNotEmpty(String(it.password)) }
                .withValidationOnApply { validateNotEmpty(String(it.password)) }
                .component
        }

        row {
            cell {
                testResultField = component(JBLabel().setAllowAutoWrapping(true).setCopyable(true))
                    .constraints(grow)
                    .component
                    .also {
                        it.isFocusable = false
                        it.isVisible = false
                    }
            }
        }
    }.withPreferredWidth(500)

    override fun createActions(): Array<out Action> {
        val testServerAction: Action = object : DialogWrapperAction("Test") {
            override fun doAction(e: ActionEvent) {
                testServer()
            }
        }

        return arrayOf(testServerAction, okAction, cancelAction)
    }

    private fun testServer() {
        val validationInfos = doValidateAll()

        updateErrorInfo(validationInfos)

        if (validationInfos.isEmpty()) {
            updateTestResult("")

            runTest(urlField.text, userField.text, String(passwordField.password))
                .onSuccess {
                    runInEdt {
                        if (it.output == "test") {
                            updateTestResult("Test success!")
                        } else {
                            updateTestResult("Test fail. Output: ${it.output}", true)
                        }
                    }
                }.onError {
                    runInEdt {
                        updateTestResult("Test fail. Error: ${it.localizedMessage}", true)
                    }
                }
        }
    }

    private fun updateTestResult(text: String, error: Boolean = false) {
        testResultField.foreground = if (error) ERROR_FOREGROUND_COLOR else null
        testResultField.text = text
        testResultField.isVisible = text.isNotBlank()
    }

    private fun runTest(url: String, user: String, password: String): Promise<GroovyConsoleOutput> {
        val promise = AsyncPromise<GroovyConsoleOutput>()

        runModalTask("Checking AEM Server Connection", project, false) {
            try {
                promise.setResult(httpService.execute(url, user, password, "print('test')".toByteArray()))
            } catch (th: Throwable) {
                thisLogger().info(th)

                promise.setError(th)
            }
        }

        return promise
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