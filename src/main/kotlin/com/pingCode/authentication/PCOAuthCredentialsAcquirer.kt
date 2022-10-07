package com.pingCode.authentication

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirerHttp
import com.intellij.util.Url
import com.pingCode.authentication.PCOAuthService.Companion.jacksonMapper
import com.pingCode.authentication.accounts.PCAccountsUtils

internal class PCOAuthCredentialsAcquirer(
    private val pcAppCred: PCAccountsUtils.PCAppCredentials,
    private val authorizationCodeUrl: Url
): OAuthCredentialsAcquirer<PCCredentials>{
    override fun acquireCredentials(code: String): OAuthCredentialsAcquirer.AcquireCredentialsResult<PCCredentials> {
        val tokenUrl = ACCESS_TOKEN_URL.addParameters(mapOf(
            "grant_type" to authGrantType,
            "client_id" to pcAppCred.clientId,
            "client_secret" to pcAppCred.clientSecret,
            "redirect_uri" to authorizationCodeUrl.toExternalForm(),
            "code" to code,
        ))

        return OAuthCredentialsAcquirerHttp.requestToken(tokenUrl) { body, _ ->
            val responseData = with(jacksonMapper) {
                propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
                readValue(body, AuthorizationResponseData::class.java)
            }

            PCCredentials(
                responseData.accessToken,
                responseData.refreshToken,
                responseData.expiresIn,
                responseData.tokenType,
                responseData.scope,
                responseData.createdAt
            )
        }
    }

    private data class AuthorizationResponseData(val accessToken: String,
                                                 val refreshToken: String,
                                                 val expiresIn: Long,
                                                 val tokenType: String,
                                                 val scope: String,
                                                 val createdAt: Long)

    companion object {
        private const val authGrantType = "authorization_code"

        private val ACCESS_TOKEN_URL: Url
            get() = PCOAuthService.SERVICE_URL.resolve("token")
    }
}