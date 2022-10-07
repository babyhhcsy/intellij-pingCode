package com.pingCode.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirerHttp
import com.intellij.collaboration.auth.services.OAuthServiceBase
import com.intellij.collaboration.auth.services.OAuthServiceWithRefresh
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.Url
import com.intellij.util.Urls
import com.pingCode.authentication.accounts.PCAccountsUtils
import java.util.concurrent.CompletableFuture

internal class PCOAuthService : OAuthServiceBase<PCCredentials>(),OAuthServiceWithRefresh<PCCredentials>{
    override val name: String get() = SERVICE_NAME
    override fun revokeToken(token: String) {
        TODO("Not yet implemented")
    }

    /**
     * 发起授权
     * */
    fun authorize(): CompletableFuture<PCCredentials> {
        val request = getPCOAuthRequest() // 获取request 请求对象，含授权对象
        return authorize(request) // 调用intellij 中的授权，他会打开浏览器
    }
    override fun updateAccessToken(refreshTokenRequest: OAuthServiceWithRefresh.RefreshTokenRequest): CompletableFuture<PCCredentials> =

        ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
            val response = OAuthCredentialsAcquirerHttp.requestToken(refreshTokenRequest.refreshTokenUrlWithParameters)

            if (response.statusCode() == 200) {
                val responseData = with(PCAccountsUtils.jacksonMapper) {
                    propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
                    readValue(response.body(), RefreshResponseData::class.java)
                }

                PCCredentials(
                    responseData.accessToken,
                    refreshTokenRequest.refreshToken,
                    responseData.expiresIn,
                    responseData.tokenType,
                    responseData.scope,
                    responseData.createdAt,
                )
            }
            else {
                throw RuntimeException(response.body().ifEmpty { "No token provided" })
            }
        }
    companion object {
        private const val SERVICE_NAME = "/v1/oauth/token"

        val jacksonMapper: ObjectMapper get() = jacksonObjectMapper()

        val instance: PCOAuthService = service()

        val SERVICE_URL: Url = Urls.newFromEncoded("https://open.pingcode.com")
    }
    private data class RefreshResponseData(val accessToken: String,
                                           val expiresIn: Long,
                                           val scope: String,
                                           val tokenType: String,
                                           val createdAt: Long)
}