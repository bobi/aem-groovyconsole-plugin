package com.github.bobi.aemgroovyconsoleplugin.config.ui

import com.github.bobi.aemgroovyconsoleplugin.services.http.GroovyConsoleHttpService
import com.github.bobi.aemgroovyconsoleplugin.services.http.model.GroovyConsoleOutput
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.UIUtil
import org.jetbrains.concurrency.AsyncPromise
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

    private lateinit var distributedExecutionField: JBCheckBox

    private lateinit var testServerAction: Action

    init {
        title = "Aem Server"

        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Server Name: ") {
            textField()
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindText({ tableItem.name }, { tableItem.name = it })
                .validationOnInput { validateNotEmpty(it.text) }
                .validationOnApply { validateNotEmpty(it.text) }
                .focused()
        }

        row("URL: ") {
            urlField = textField()
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindText({ tableItem.url }, { tableItem.url = it })
                .validationOnInput { validateUrl(it.text) }
                .validationOnApply { validateUrl(it.text) }
                .component
        }

        row("Username: ") {
            userField = textField()
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindText({ tableItem.user }, { tableItem.user = it })
                .validationOnInput { validateNotEmpty(it.text) }
                .validationOnApply { validateNotEmpty(it.text) }
                .component
        }

        row("Password: ") {
            passwordField = cell(JPasswordField())
                .columns(COLUMNS_SHORT)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bind(
                    JPasswordField::getPassword,
                    { field, value -> field.text = String(value) },
                    MutableProperty({ tableItem.password.toCharArray() }, { tableItem.password = String(it) })
                )
                .validationOnInput { validateNotEmpty(String(it.password)) }
                .validationOnApply { validateNotEmpty(String(it.password)) }
                .component
        }

        row {
            distributedExecutionField = checkBox("Enable distributed execution")
                .comment("OSGI 'distributedExecutionEnabled' property must be set to 'true'.", MAX_LINE_LENGTH_NO_WRAP)
                .horizontalAlign(HorizontalAlign.FILL)
                .resizableColumn()
                .bindSelected({ tableItem.distributedExecution }, { tableItem.distributedExecution = it })
                .component
        }

        row {
            testResultField = label("")
                .horizontalAlign(HorizontalAlign.FILL)
                .verticalAlign(VerticalAlign.FILL)
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
                    runInEdt { updateTestResult("Test fail. Error: ${ex.localizedMessage}", true) }
                }
            }

            val req = object {
                val url = urlField.text
                val user = userField.text
                val password = String(passwordField.password)
                val script = "print('test')".toByteArray()
            }

            runModalTask("Checking AEM Server Connection", project, false) {
                try {
                    promise.setResult(
                        httpService.execute(
                            req.url,
                            req.user,
                            req.password,
                            req.script,
                            GroovyConsoleHttpService.Action.EXECUTE
                        )
                    )
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