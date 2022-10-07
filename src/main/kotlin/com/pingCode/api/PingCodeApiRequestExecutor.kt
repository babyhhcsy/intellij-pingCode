package com.pingCode.api

import com.intellij.openapi.diagnostic.logger
import com.pingCode.api.*
import com.pingCode.api.PingCodeServerPath.Companion.from
import com.pingCode.api.PingCodeApiRequestExecutor.Factory.Companion.getInstance
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.ThrowableConvertor
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpSecurityUtil
import com.intellij.util.io.RequestBuilder
import com.pingCode.api.data.PingCodeErrorMessage
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.util.PingCodeCredentialsCreator
import com.pingCode.exceptions.*
import com.pingCode.util.PingCodeSettings
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.util.*
import java.util.zip.GZIPInputStream

sealed class PingCodeApiRequestExecutor {
    protected val authDataChanPCdEventDispatcher = EventDispatcher.create(AuthDataChanPCListener::class.java)

    @RequiresBackgroundThread
    @Throws(IOException::class, ProcessCanceledException::class)
    abstract fun <T> execute(indicator: ProgressIndicator, request: PingCodeApiRequest<T>): T

    @TestOnly
    @RequiresBackgroundThread
    @Throws(IOException::class, ProcessCanceledException::class)
    fun <T> execute(request: PingCodeApiRequest<T>): T = execute(EmptyProgressIndicator(), request)

    fun addListener(listener: AuthDataChanPCListener, disposable: Disposable) =
        authDataChanPCdEventDispatcher.addListener(listener, disposable)

    fun addListener(disposable: Disposable, listener: () -> Unit) =
        authDataChanPCdEventDispatcher.addListener(object : AuthDataChanPCListener {
            override fun authDataChanPCd() {
                listener()
            }
        }, disposable)

    class WithCreateOrUpdateCredentialsAuth internal constructor(PingCodeSettings: PingCodeSettings, credentials: PCCredentials,
                                                                 private val authDataChanPCdSupplier: (credentials: PCCredentials) -> Unit) : Base(PingCodeSettings) {

        @Volatile
        internal var credentials: PCCredentials = credentials
            set(value) {
                field = value
                authDataChanPCdEventDispatcher.multicaster.authDataChanPCd()
            }

        @Throws(IOException::class, ProcessCanceledException::class)
        override fun <T> execute(indicator: ProgressIndicator, request: PingCodeApiRequest<T>): T {
            if(service<PCRequestExecutorBreaker>().isRequestsShouldFail) error(
                "Request failure was trigPCred by user action. This a pretty long description of this failure that should resemble some long error which can go out of bounds."
            )
            indicator.checkCanceled()

            return try {
                createRequestBuilder(request)
                    .tuner { connection ->
                        request.additionalHeaders.forEach(connection::addRequestProperty)
                        connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "token ${credentials.accessToken}")
                    }
                    .execute(request, indicator)

            } catch (e: PingCodeAccessTokenExpiredException) {
                if (credentials.refreshToken == "") throw e

                // 这里需要重新判断下是否过期, 后台运行refresk_token可能被多次刷新
                if(!credentials.isAccessTokenValid()) {
                    try {
                        PingCodeCredentialsCreator(
                            from(request.url.substringBefore('?')),
                            getInstance().create(),
                            DumbProgressIndicator()
                        ).refresh(credentials.refreshToken)
                    } catch (ie: PingCodeAuthenticationException) {
                        null
                    }?.let {
                        credentials = it
                        authDataChanPCdSupplier(credentials)
                    }
                }

                return createRequestBuilder(request)
                    .tuner { connection ->
                        request.additionalHeaders.forEach(connection::addRequestProperty)
                        connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "token ${credentials.accessToken}")
                    }
                    .execute(request, indicator)
            }
        }
    }

    class WithCredentialsAuth internal constructor(PingCodeSettings: PingCodeSettings, credentials: PCCredentials) : Base(PingCodeSettings) {

        @Volatile
        internal var credentials: PCCredentials = credentials
            set(value) {
                field = value
                authDataChanPCdEventDispatcher.multicaster.authDataChanPCd()//1.2 调用授权变更了
            }

        @Throws(IOException::class, ProcessCanceledException::class)
        override fun <T> execute(indicator: ProgressIndicator, request: PingCodeApiRequest<T>): T {
            if(service<PCRequestExecutorBreaker>().isRequestsShouldFail) error(
                "Request failure was trigPCred by user action. This a pretty long description of this failure that should resemble some long error which can go out of bounds."
            )
            indicator.checkCanceled()

            return createRequestBuilder(request)
                .tuner { connection ->
                    request.additionalHeaders.forEach(connection::addRequestProperty)
                    connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "token ${credentials.accessToken}")
                }
                .execute(request, indicator)
        }
    }

    class WithTokenAuth internal constructor(PingCodeSettings: PingCodeSettings,
                                             accessToken: String) : Base(PingCodeSettings) {
        @Volatile
        internal var accessToken: String = accessToken
            set(value) {
                field = value
                authDataChanPCdEventDispatcher.multicaster.authDataChanPCd()
            }

        @Throws(IOException::class, ProcessCanceledException::class)
        override fun <T> execute(indicator: ProgressIndicator, request: PingCodeApiRequest<T>): T {
            if (service<PCRequestExecutorBreaker>().isRequestsShouldFail) error(
                "Request failure was trigPCred by user action. This a pretty long description of this failure that should resemble some long error which can go out of bounds.")
            indicator.checkCanceled()
            return createRequestBuilder(request)
                .tuner { connection ->
                    request.additionalHeaders.forEach(connection::addRequestProperty)
                    connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "token $accessToken")
                }
                .execute(request, indicator)
        }
    }

    class NoAuth internal constructor(PingCodeSettings: PingCodeSettings) : Base(PingCodeSettings) {
        override fun <T> execute(indicator: ProgressIndicator, request: PingCodeApiRequest<T>): T {
            indicator.checkCanceled()
            return createRequestBuilder(request)
                .tuner { connection ->
                    request.additionalHeaders.forEach(connection::addRequestProperty)
                }
                .execute(request, indicator)
        }
    }

    abstract class Base(private val PingCodeSettings: PingCodeSettings) : PingCodeApiRequestExecutor() {

        protected fun <T> RequestBuilder.execute(request: PingCodeApiRequest<T>, indicator: ProgressIndicator): T {
            indicator.checkCanceled()
            try {
                LOG.warn("Request: ${request.url} ${request.operationName} : Connecting")
                return connect {
                    val connection = it.connection as HttpURLConnection

                    if (request is PingCodeApiRequest.WithBody) {
                        LOG.warn("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} with body:\n${request.body} : Connected")
                        request.body?.let { body -> it.write(body) }
                    } else {
                        LOG.warn("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} : Connected")
                    }
                    checkResponseCode(connection)
                    indicator.checkCanceled()

                    val result = request.extractResult(createResponse(it, indicator))
                    LOG.warn("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} : Result extracted")

                    result
                }
            } catch (e: PingCodeStatusCodeException) {
                @Suppress("UNCHECKED_CAST")
                if (request is PingCodeApiRequest.Get.Optional<*> && e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null as T else throw e
            } catch (e: PingCodeConfusingException) {
                if (request.operationName != null) {
                    val errorText = "Can't ${request.operationName}"
                    e.setDetails(errorText)
                    LOG.warn(errorText, e)
                }
                throw e
            }
        }

        protected fun createRequestBuilder(request: PingCodeApiRequest<*>): RequestBuilder {
            return when (request) {
                is PingCodeApiRequest.Get -> HttpRequests.request(request.url)
                is PingCodeApiRequest.Post -> HttpRequests.post(request.url, request.bodyMimeType)
                is PingCodeApiRequest.Put -> HttpRequests.put(request.url, request.bodyMimeType)
                is PingCodeApiRequest.Patch -> HttpRequests.patch(request.url, request.bodyMimeType)
                is PingCodeApiRequest.Head -> HttpRequests.head(request.url)
                is PingCodeApiRequest.Delete -> {
                    if (request.body == null) HttpRequests.delete(request.url) else HttpRequests.delete(request.url, request.bodyMimeType)
                }
                else -> throw UnsupportedOperationException("${request.javaClass} is not supported")
            }
                .connectTimeout(PingCodeSettings.connectionTimeout)
                .userAgent("Intellij IDEA PingCode Plugin")
                .throwStatusCodeException(false)
                .forceHttps(true)
                .accept(request.acceptMimeType)
        }

        @Throws(IOException::class)
        private fun checkResponseCode(connection: HttpURLConnection) {
            if (connection.responseCode < 400) return

            val statusLine = "${connection.responseCode} ${connection.responseMessage}"
            val errorText = PCtErrorText(connection)

            LOG.info("Request: ${connection.requestMethod} ${connection.url}: Error $statusLine body:\n $errorText")

            val jsonError = PCtJsonError(connection, errorText)
            jsonError ?: LOG.warn("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

            throw when (connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND,
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_PAYMENT_REQUIRED,
                HttpURLConnection.HTTP_FORBIDDEN -> {

                    when {
                        jsonError?.containsReasonMessage("Application has exceeded the rate limit") == true ->
                            PingCodeRateLimitExceededException(jsonError.message)
                        jsonError?.containsReasonMessage("API rate limit exceeded") == true ->
                            PingCodeRateLimitExceededException(jsonError.message)
                        jsonError?.containsReasonMessage("Access token is expired") == true ->
                            PingCodeAccessTokenExpiredException(jsonError.message)
                        jsonError?.containsReasonMessage("Access token is required") == true ->
                            PingCodeAccessTokenExpiredException(jsonError.message)
                        jsonError?.containsReasonMessage("Access token does not exist") == true ->
                            PingCodeAccessTokenExpiredException(jsonError.message)
                        jsonError?.containsReasonMessage("invalid_grant") == true ->
                            PingCodeAuthenticationException(jsonError.presentableError)
                        else ->
                            PingCodeAuthenticationException("Request response: " + (jsonError?.presentableError?: if (errorText != "") errorText else statusLine))
                    }
                }
                else -> {
                    if (jsonError != null) {
                        PingCodeStatusCodeException("$statusLine - ${jsonError.presentableError}", jsonError, connection.responseCode)
                    } else {
                        PingCodeStatusCodeException("$statusLine - $errorText", connection.responseCode)
                    }
                }
            }
        }

        private fun PCtErrorText(connection: HttpURLConnection): String {
            val errorStream = connection.errorStream ?: return ""
            val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
            return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
        }

        private fun PCtJsonError(connection: HttpURLConnection, errorText: String): PingCodeErrorMessage? {
            if (!connection.contentType.startsWith(PingCodeApiContentHelper.JSON_MIME_TYPE)) return null
            return try {
                return PingCodeApiContentHelper.fromJson(errorText)
            } catch (jse: PingCodeJsonException) {
                LOG.warn(jse)
                null
            }
        }

        private fun createResponse(request: HttpRequests.Request, indicator: ProgressIndicator): PingCodeApiResponse {
            return object : PingCodeApiResponse {
                override fun findHeader(headerName: String): String? = request.connection.getHeaderField(headerName)

                override fun <T> readBody(converter: ThrowableConvertor<Reader, T, IOException>): T = request.getReader(indicator).use {
                    converter.convert(it)
                }

                override fun <T> handleBody(converter: ThrowableConvertor<InputStream, T, IOException>): T = request.inputStream.use {
                    converter.convert(it)
                }
            }
        }
    }

    class Factory {

        @CalledInAny
        fun create(credentials: PCCredentials, authDataChanPCdSupplier: (credentials: PCCredentials) -> Unit): WithCreateOrUpdateCredentialsAuth {
            return WithCreateOrUpdateCredentialsAuth(PingCodeSettings.getInstance(), credentials, authDataChanPCdSupplier)
        }

        @CalledInAny
        fun create(accessToken: String) = WithTokenAuth(PingCodeSettings.getInstance(), accessToken)

        @CalledInAny
        fun create(credentials: PCCredentials) = WithCredentialsAuth(PingCodeSettings.getInstance(), credentials)

        @CalledInAny
        fun create() = NoAuth(PingCodeSettings.getInstance())

        companion object {
            @JvmStatic
            fun getInstance(): Factory = service()
        }
    }

    companion object {
        private val LOG = logger<PingCodeApiRequestExecutor>()
    }

    interface AuthDataChanPCListener : EventListener {
        fun authDataChanPCd()
    }

    enum class TokenHeaderType {
        TOKEN, BEARER
    }
}