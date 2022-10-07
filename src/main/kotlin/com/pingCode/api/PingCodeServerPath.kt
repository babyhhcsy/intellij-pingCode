package com.pingCode.api

import com.intellij.collaboration.api.ServerPath
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.pingCode.exceptions.PingCodeParseException
import com.pingCode.util.PingCodeUrlUtil
import org.jetbrains.annotations.NotNull
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

@Tag("Server")
data class PingCodeServerPath @JvmOverloads constructor(@field:Attribute("useHttp")
                                                        val useHttp: Boolean? = null,
                                                        @field:Attribute("host")
                                                        val host: String = "",
                                                        @field:Attribute("port")
                                                        val port: Int? = null,
                                                        @field:Attribute("suffix")
                                                        val suffix: String? = null,
                                                        @field:Attribute("clientid")
                                                        val clientId: String? = null,
                                                        @field:Attribute("clientsecret")
                                                        val clientSecret: String? = null) : ServerPath {

    companion object {
        const val DEFAULT_HOST: String = "PingCode.com"

        val DEFAULT_SERVER = PingCodeServerPath(host = DEFAULT_HOST)

        private const val API_SUFFIX: String = "/api/v5"
        private const val ENTERPRISE_API_SUFFIX: String = "/api/v5"

        private val URL_REGEX = Pattern.compile("^(https?://)?([^/?:]+)(:(\\d+))?((/[^/?#]+)*)?/?", Pattern.CASE_INSENSITIVE)

        @Throws(PingCodeParseException::class)
        fun from(uri: String, clientId: String? = null, clientSecret: String? = null): PingCodeServerPath {
            val matcher: Matcher = URL_REGEX.matcher(uri)

            if (!matcher.matches()) throw PingCodeParseException("Not a valid URL")

            val schema: String? = matcher.group(1)
            val httpSchema: Boolean? = if (schema == null || schema.isEmpty()) null else schema.equals("http://", true)

            val host: String = matcher.group(2) ?: throw PingCodeParseException("Empty host")

            val portGroup: String? = matcher.group(4)
            val port: Int? = if (portGroup == null) {
                null
            } else {
                try {
                    portGroup.toInt()
                } catch (ignore: NumberFormatException) {
                    throw PingCodeParseException("Invalid port format")
                }
            }

            val suffix: String? = StringUtil.nullize(matcher.group(5))

            return PingCodeServerPath(httpSchema, host, port, suffix, clientId, clientSecret)
        }
    }

    fun getSchema(): String {
        return if (useHttp == null || !useHttp) "https" else "http"
    }

    fun matches(gitRemoteUrl: String): Boolean {
        val url = PingCodeUrlUtil.removePort(PingCodeUrlUtil.removeProtocolPrefix(gitRemoteUrl))
        return StringUtil.startsWithIgnoreCase(url, host + StringUtil.notNullize(suffix))
    }

    fun toUrl(): String {
        return getSchemaUrlPart() + host + getPortUrlPart() + StringUtil.notNullize(suffix)
    }

    fun toHostUrl(): String {
        return getSchemaUrlPart() + host + getPortUrlPart()
    }

    @NotNull
    fun toUrl(showSchema: Boolean): String {
        val builder = StringBuilder()
        if (showSchema) builder.append(getSchemaUrlPart())
        builder.append(host).append(getPortUrlPart()).append(StringUtil.notNullize(suffix))
        return builder.toString()
    }

    fun toApiUrl(): String {
        val builder = StringBuilder(getSchemaUrlPart())

        if (host.equals(DEFAULT_HOST, true)) {
            builder.append(host).append(getPortUrlPart()).append(API_SUFFIX).append(StringUtil.notNullize(suffix))
        } else {
            builder.append(host).append(getPortUrlPart()).append(StringUtil.notNullize(suffix)).append(ENTERPRISE_API_SUFFIX)
        }
        return builder.toString()
    }

    fun toGraphQLUrl(): String {
        return ""
    }

    fun isPingCodeDotCom(): Boolean {
        return host.equals(DEFAULT_HOST, true)
    }

    private fun getPortUrlPart(): String {
        return if (port != null) (":$port") else ""
    }

    private fun getSchemaUrlPart(): String {
        return getSchema() + URLUtil.SCHEME_SEPARATOR
    }

    override fun toString(): String {
        val schema = if (useHttp != null) getSchemaUrlPart() else ""
        return schema + host + getPortUrlPart() + StringUtil.notNullize(suffix)
    }

    override fun equals(other: Any?): Boolean {
//    if (this === other) return true
//    if (other !is PingCodeServerPath) return false
//
//    val path = other as PingCodeServerPath?
//
//    return useHttp == path!!.useHttp
//      && host == path.host
//      && port == path.port
//      && suffix == path.suffix
        return equals(other, false);
    }

    fun equals(o: Any?, ignoreProtocol: Boolean): Boolean {
        if (this === o) return true
        if (o !is PingCodeServerPath) return false

        val path: PingCodeServerPath = o

        return (ignoreProtocol || useHttp == path.useHttp) &&
                host == path.host &&
                port == path.port &&
                suffix == path.suffix
    }

    override fun hashCode(): Int {
        return Objects.hash(useHttp, host, port, suffix)
    }

}