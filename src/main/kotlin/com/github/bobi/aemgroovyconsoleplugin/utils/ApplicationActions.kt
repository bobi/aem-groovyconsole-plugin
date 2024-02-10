package com.github.bobi.aemgroovyconsoleplugin.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationManagerEx

fun <T> invokeAndWaitIfNeeded(modalityState: ModalityState? = null, runnable: () -> T): T {
    val app = ApplicationManager.getApplication()
    if (app.isDispatchThread) {
        return runnable()
    } else {
        var resultRef: T? = null
        app.invokeAndWait({ resultRef = runnable() }, modalityState ?: ModalityState.defaultModalityState())
        @Suppress("UNCHECKED_CAST")
        return resultRef as T
    }
}

fun isInternal(): Boolean {
    return java.lang.Boolean.getBoolean(ApplicationManagerEx.IS_INTERNAL_PROPERTY)
}
