package com.pingCode.authentication

import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer
import com.intellij.collaboration.auth.services.OAuthRequest
import com.intellij.util.Url
import com.intellij.util.Urls
import com.pingCode.authentication.accounts.PCAccountsUtils
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService

internal fun getPCOAuthRequest():PCOAuthRequest{
    val pcAppCred = PCAccountsUtils.getDefaultPCAppCredentials()
    return PCOAuthRequest(pcAppCred)
}
internal class PCOAuthRequest(giteeAppCred: PCAccountsUtils.PCAppCredentials): OAuthRequest<PCCredentials> {
    private val port: Int get() = BuiltInServerManager.getInstance().port

    override val authorizationCodeUrl: Url
        get() = Urls.newFromEncoded("http://127.0.0.1:$port/${RestService.PREFIX}/${PCOAuthService.instance.name}/authorization_code")

    override val credentialsAcquirer: OAuthCredentialsAcquirer<PCCredentials> =
        PCOAuthCredentialsAcquirer(giteeAppCred, authorizationCodeUrl)

    override val authUrlWithParameters: Url = AUTHORIZE_URL.addParameters(mapOf(
        "client_id" to giteeAppCred.clientId,
        "scope" to PCAccountsUtils.APP_CLIENT_SCOPE,
        "redirect_uri" to authorizationCodeUrl.toExternalForm(),
        "response_type" to responseType,
    ))
    companion object {
        private const val responseType = "code"

        private val AUTHORIZE_URL: Url
            get() = PCOAuthService.SERVICE_URL.resolve("authorize")
    }

}