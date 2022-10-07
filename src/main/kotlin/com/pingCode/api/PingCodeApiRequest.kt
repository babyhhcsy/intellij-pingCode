package com.pingCode.api

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.util.ThrowableConvertor
import com.pingCode.api.data.PingCodeResponsePage
import com.pingCode.exceptions.PingCodeConfusingException
import java.io.IOException

sealed class PingCodeApiRequest<out T>(val url: String) {
    var operationName: String? = null
    abstract val acceptMimeType: String?

    open val tokenHeaderType = PingCodeApiRequestExecutor.TokenHeaderType.TOKEN

    protected val headers = mutableMapOf<String, String>()
    val additionalHeaders: Map<String, String>
        get() = headers

    @Throws(IOException::class)
    abstract fun extractResult(response: PingCodeApiResponse): T

    fun withOperationName(name: String): PingCodeApiRequest<T> {
        operationName = name
        return this
    }

    abstract class Get<T> @JvmOverloads constructor(url: String,
                                                    override val acceptMimeType: String? = null) : PingCodeApiRequest<T>(url) {

        abstract class Optional<T> @JvmOverloads constructor(url: String,
                                                             acceptMimeType: String? = null) : Get<T?>(url, acceptMimeType) {
            companion object {
                inline fun <reified T> json(url: String, acceptMimeType: String? = null): Optional<T> =
                    Json(url, T::class.java, acceptMimeType)
            }

            open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
                : Optional<T>(url, acceptMimeType) {

                override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
            }
        }

        companion object {
            inline fun <reified T> json(url: String, acceptMimeType: String? = null): Get<T> =
                Json(url, T::class.java, acceptMimeType)

            inline fun <reified T> jsonPage(url: String, acceptMimeType: String? = null): Get<PingCodeResponsePage<T>> =
                JsonPage(url, T::class.java, acceptMimeType)

            inline fun <reified T> jsonSearchPage(url: String, acceptMimeType: String? = null): Get<PingCodeResponsePage<T>> =
                JsonSearchPage(url, T::class.java, acceptMimeType)

            // requests for PingCode
            inline fun <reified T> jsonList(url: String): Get<List<T>> = JsonList(url, T::class.java)

        }

        open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Get<T>(url, acceptMimeType) {

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }

        open class JsonList<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Get<List<T>>(url, acceptMimeType) {

            override fun extractResult(response: PingCodeApiResponse): List<T> = parseJsonList(response, clazz)
        }

        open class JsonPage<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Get<PingCodeResponsePage<T>>(url, acceptMimeType) {

            override fun extractResult(response: PingCodeApiResponse): PingCodeResponsePage<T> {
                return PingCodeResponsePage.parseFromHeaderPage(parseJsonList(response, clazz), url,
                    response.findHeader(PingCodeResponsePage.HEADER_TOTAL_PAGE)?.toInt())
            }
        }

        open class JsonSearchPage<T>(url: String,
                                     private val clazz: Class<T>,
                                     acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Get<PingCodeResponsePage<T>>(url, acceptMimeType) {

            override fun extractResult(response: PingCodeApiResponse): PingCodeResponsePage<T> {
                return PingCodeResponsePage.parseFromHeaderPage(parseJsonList(response, clazz), url,
                    response.findHeader(PingCodeResponsePage.HEADER_TOTAL_PAGE)?.toInt())
            }
        }
    }

    abstract class Head<T> @JvmOverloads constructor(url: String,
                                                     override val acceptMimeType: String? = null) : PingCodeApiRequest<T>(url)

    abstract class WithBody<out T>(url: String) : PingCodeApiRequest<T>(url) {
        abstract val body: String?
        abstract val bodyMimeType: String
    }

    abstract class Post<out T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                         url: String,
                                                         override val acceptMimeType: String? = null) : PingCodeApiRequest.WithBody<T>(url) {
        companion object {
            inline fun <reified T> json(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
                Json(url, body, T::class.java, acceptMimeType)

            inline fun <reified T> formUrlEncoded(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
                FormUrlEncoded(url, body, T::class.java, acceptMimeType)
        }

        open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                           acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Post<T>(PingCodeApiContentHelper.JSON_MIME_TYPE, url, acceptMimeType) {

            override val body: String
                get() = PingCodeApiContentHelper.toJson(bodyObject)

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }

        open class FormUrlEncoded<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                                     acceptMimeType: String? = PingCodeApiContentHelper.JSON_MIME_TYPE)
            : Post<T>(PingCodeApiContentHelper.FORM_URLENCODED_MINE_TYPE, url, acceptMimeType) {

            override val body: String
                get() = PingCodeApiContentHelper.toFormUrlEncoded(bodyObject)

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }
    }

    abstract class Put<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                    url: String,
                                                    override val acceptMimeType: String? = null) : PingCodeApiRequest.WithBody<T>(url) {
        companion object {
            inline fun <reified T> json(url: String, body: Any? = null): Put<T> = Json(url, body, T::class.java)

            inline fun <reified T> jsonList(url: String, body: Any): Put<List<T>> = JsonList(url, body, T::class.java)
        }

        open class Json<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
            : Put<T>(PingCodeApiContentHelper.JSON_MIME_TYPE, url, PingCodeApiContentHelper.JSON_MIME_TYPE) {
            init {
                if (bodyObject == null) headers["Content-Length"] = "0"
            }

            override val body: String?
                get() = bodyObject?.let { PingCodeApiContentHelper.toJson(it) }

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }

        open class JsonList<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
            : Put<List<T>>(PingCodeApiContentHelper.JSON_MIME_TYPE, url, PingCodeApiContentHelper.JSON_MIME_TYPE) {
            init {
                if (bodyObject == null) headers["Content-Length"] = "0"
            }

            override val body: String?
                get() = bodyObject?.let { PingCodeApiContentHelper.toJson(it) }

            override fun extractResult(response: PingCodeApiResponse): List<T> = parseJsonList(response, clazz)
        }
    }

    abstract class Patch<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                      url: String,
                                                      override val acceptMimeType: String? = null)
        : PingCodeApiRequest.WithBody<T>(url) {

        companion object {
            inline fun <reified T> json(url: String, body: Any): Patch<T> = Json(url, body, T::class.java)
        }

        open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>)
            : Patch<T>(PingCodeApiContentHelper.JSON_MIME_TYPE, url, PingCodeApiContentHelper.JSON_MIME_TYPE) {

            override val body: String?
                get() = bodyObject.let { PingCodeApiContentHelper.toJson(it) }

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }
    }

    abstract class Delete<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                       url: String,
                                                       override val acceptMimeType: String? = null) : PingCodeApiRequest.WithBody<T>(url) {

        companion object {
            inline fun <reified T> json(url: String, body: Any? = null): Delete<T> = Json(url, body, T::class.java)
        }

        open class Json<T>(url: String, private val bodyObject: Any? = null, private val clazz: Class<T>)
            : Delete<T>(PingCodeApiContentHelper.JSON_MIME_TYPE, url, PingCodeApiContentHelper.JSON_MIME_TYPE) {
            init {
                if (bodyObject == null) headers["Content-Length"] = "0"
            }

            override val body: String?
                get() = bodyObject?.let { PingCodeApiContentHelper.toJson(it) }

            override fun extractResult(response: PingCodeApiResponse): T = parseJsonObject(response, clazz)
        }
    }

    companion object {
        private fun <T> parseJsonObject(response: PingCodeApiResponse, clazz: Class<T>): T {
            return response.readBody(ThrowableConvertor { PingCodeApiContentHelper.readJsonObject(it, clazz) })
        }

        private fun <T> parseJsonList(response: PingCodeApiResponse, clazz: Class<T>): List<T> {
            return response.readBody(ThrowableConvertor { PingCodeApiContentHelper.readJsonList(it, clazz) })
        }
    }

}
