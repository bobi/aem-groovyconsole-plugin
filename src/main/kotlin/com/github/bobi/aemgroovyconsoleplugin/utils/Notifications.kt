package com.github.bobi.aemgroovyconsoleplugin.utils

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

private const val NOTOFICATION_GROUP_ID = "AEM Groovy Console"

/**
 * User: Andrey Bardashevsky
 * Date/Time: 08.03.2023 15:23
 */
object Notifications {
    fun notifyError(title: String = "", content: String) {
        notify(title, content, NotificationType.ERROR)
    }

    fun notifyInfo(title: String = "", content: String) {
        notify(title, content, NotificationType.INFORMATION)
    }

    fun notifyWarn(title: String = "", content: String) {
        notify(title, content, NotificationType.WARNING)
    }

    private fun notify(title: String, content: String, type: NotificationType) {
        Notifications.Bus.notify(Notification(NOTOFICATION_GROUP_ID, title, content, type))
    }
}