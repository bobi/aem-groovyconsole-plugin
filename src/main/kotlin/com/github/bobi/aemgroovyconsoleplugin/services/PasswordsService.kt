package com.github.bobi.aemgroovyconsoleplugin.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe


private const val AEM_ACCESS_TOKEN_SUFFIX = "-aem-access-token"

/**
 * User: Andrey Bardashevsky
 * Date/Time: 29.07.2022 22:41
 */
object PasswordsService {
    private const val AEM_GROOVY_CONSOLE = "AEMGroovyConsole"

    fun removeCredentials(id: Long) {
        val passwordSafe = PasswordSafe.instance
        passwordSafe[credentialAttributes(id)] = null
        passwordSafe[credentialAttributes(id, AEM_ACCESS_TOKEN_SUFFIX)] = null
    }

    fun setCredentials(id: Long, user: String, password: String) {
        val passwordSafe = PasswordSafe.instance

        passwordSafe[credentialAttributes(id)] = Credentials(user, password)
        passwordSafe[credentialAttributes(id, AEM_ACCESS_TOKEN_SUFFIX)] = null
    }

    fun getCredentials(id: Long): Credentials? = PasswordSafe.instance[credentialAttributes(id)]

    fun setAccessToken(id: Long, token: String) {
        PasswordSafe.instance[credentialAttributes(id, AEM_ACCESS_TOKEN_SUFFIX)] = Credentials("token", token)
    }

    fun getAccessToken(id: Long): String? =
        PasswordSafe.instance[credentialAttributes(id, AEM_ACCESS_TOKEN_SUFFIX)]?.getPasswordAsString()

    private fun credentialAttributes(id: Long, suffix: String = "") = CredentialAttributes(
        generateServiceName(AEM_GROOVY_CONSOLE, id.toString() + suffix)
    )
}
