package com.pingCode.authentication.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.PCOAuthService
import com.pingCode.authentication.getPCOAuthRequest
import java.util.concurrent.CompletableFuture

internal object PCAccountsUtils {
    data class PCAppCredentials(val clientId: String, val clientSecret: String)

    private val Log = logger<PCOAuthService>()

    val jacksonMapper : ObjectMapper get() = jacksonObjectMapper();

    const val APP_CLIENT_ID: String = "AerkFujOSMyI"
    const val APP_CLIENT_SECRET: String = "ClRrflcQnafEWxrYAMYXZaCd"
    const val APP_CLIENT_SCOPE: String = ""

    fun getDefaultPCAppCredentials(): PCAppCredentials{
        return PCAppCredentials(APP_CLIENT_ID, APP_CLIENT_SECRET)
    }
    fun tryToReLogin(project: Project): PCCredentials?{
        var credentialsFuture : CompletableFuture<PCCredentials> = CompletableFuture()
        return try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable{
                val request = getPCOAuthRequest()
                credentialsFuture = PCOAuthService.instance.authorize(request)
                ProgressIndicatorUtils.awaitWithCheckCanceled(credentialsFuture)
            },"登录pingCode",true,project)
        }catch (t: Throwable){
            credentialsFuture.cancel(true)
            null
        }
    }
}