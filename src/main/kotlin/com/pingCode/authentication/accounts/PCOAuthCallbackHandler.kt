// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.accounts

import com.intellij.collaboration.auth.OAuthCallbackHandlerBase
import com.intellij.collaboration.auth.services.OAuthService
import com.intellij.util.Urls
import com.intellij.util.io.isLocalOrigin
import com.intellij.util.io.referrer
import com.pingCode.authentication.PCOAuthService
import com.pingCode.authentication.PingCodeServerPath
import io.netty.handler.codec.http.HttpRequest

internal class PCOAuthCallbackHandler : OAuthCallbackHandlerBase() {
  override fun oauthService(): OAuthService<*> = PCOAuthService.instance
  /**
   * 用户授权同意后执行
   * */
  override fun handleAcceptCode(isAccepted: Boolean): AcceptCodeHandleResult {
    val redirectUrl = if (isAccepted) {
      PCOAuthService.SERVICE_URL.resolve("intellij/complete")
    } else {
      PCOAuthService.SERVICE_URL.resolve("intellij/error")
    }
    return AcceptCodeHandleResult.Redirect(redirectUrl)
  }

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    if (request.isLocalOrigin()) return OriginCheckResult.ALLOW

    val uri = request.referrer ?: return OriginCheckResult.ALLOW

    try {
      val parsedUri = Urls.parse(uri, false) ?: return OriginCheckResult.FORBID

      return if (parsedUri.authority == PingCodeServerPath.DEFAULT_HOST)
        OriginCheckResult.ALLOW else OriginCheckResult.FORBID

    } catch (ignored: Exception) {
    }

    return OriginCheckResult.FORBID
  }
}