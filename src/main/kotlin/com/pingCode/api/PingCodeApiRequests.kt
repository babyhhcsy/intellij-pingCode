/*
 *  Copyright 2016-2019 码云 - PingCode
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pingCode.api

import com.intellij.util.ThrowableConvertor
import com.pingCode.api.data.PingCodeAuthorization
import com.pingCode.api.data.request.PingCodeAuthenticatedUser
import com.pingCode.api.requests.AuthorizationCreateRequest
import com.pingCode.api.requests.AuthorizationUpdateRequest
import com.pingCode.authentication.PCCredentials
import java.awt.Image

/**
 * Collection of factory methods for API requests used in plugin
 * TODO: improve url building (DSL?)
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/PingCodeApiRequests.kt
 * @author JetBrains s.r.o.
 */
object PingCodeApiRequests {
  object CurrentUser : Entity("/user") {
    @JvmStatic
    fun get(server: PingCodeServerPath) = get(getUrl(server, urlSuffix))

    @JvmStatic
    fun get(url: String) =
      PingCodeApiRequest.Get.json<PingCodeAuthenticatedUser>(url).withOperationName("get profile information")

    @JvmStatic
    fun getAvatar(url: String) = object : PingCodeApiRequest.Get<Image>(url) {
      override fun extractResult(response: PingCodeApiResponse): Image {
        return response.handleBody(ThrowableConvertor {
          PingCodeApiContentHelper.loadImage(it)
        })
      }
    }.withOperationName("get profile avatar")

  }

  object Auth : Entity("/oauth/token") {
    @JvmStatic
    fun create(server: PingCodeServerPath, scope: String, login: String, password: CharArray) =
      PingCodeApiRequest.Post.formUrlEncoded<PCCredentials>(
        getBaseUrl(server, urlSuffix),
        AuthorizationCreateRequest(scope, login, String(password), server.clientId, server.clientSecret)
      ).withOperationName("create authorization")

    @JvmStatic
    fun update(server: PingCodeServerPath, refreshToken: String) =
      PingCodeApiRequest.Post.formUrlEncoded<PCCredentials>(
        getBaseUrl(server, urlSuffix),
        AuthorizationUpdateRequest(refreshToken)
      ).withOperationName("update authorization")

    @JvmStatic
    fun get(server: PingCodeServerPath) = PingCodeApiRequest.Get.jsonList<PingCodeAuthorization>(getUrl(server, urlSuffix))
      .withOperationName("get authorizations")
  }
  object Repos : Entity("/repos") {

  }
  abstract class Entity(val urlSuffix: String)

  private fun getBaseUrl(server: PingCodeServerPath, suffix: String) = server.toHostUrl() + suffix

  private fun getUrl(repository: PCRepositoryCoordinates, vararg suffixes: String) =
      getUrl(repository.serverPath, Repos.urlSuffix, "/", repository.repositoryPath.toString(), *suffixes)

  fun getUrl(server: PingCodeServerPath, vararg suffixes: String) = StringBuilder(server.toApiUrl()).append(*suffixes).toString()

  private fun getQuery(vararg queryParts: String): String {
    val builder = StringBuilder()
    for (part in queryParts) {
      if (part.isEmpty()) continue
      if (builder.isEmpty()) builder.append("?")
      else builder.append("&")
      builder.append(part)
    }
    return builder.toString()
  }
}