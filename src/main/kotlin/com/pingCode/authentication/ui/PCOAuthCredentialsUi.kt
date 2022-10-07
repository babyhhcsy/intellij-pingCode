// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui

import PingCodeApiRequestExecutor
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.PCOAuthService
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.ui.UIUtil.getInactiveTextColor
import com.pingCode.authentication.util.Validator
import javax.swing.JComponent

internal class PCOAuthCredentialsUi(
  val factory: PingCodeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : PCCredentialsUi() {

  override fun getPreferredFocusableComponent(): JComponent? = null

  override fun getValidator(): Validator = { null }

  override fun createExecutor(): PingCodeApiRequestExecutor = factory.create(PCCredentials.EmptyCredentials)
  // 授权登录系统
  override fun acquireLoginAndToken(
    server: PingCodeServerPath,
    executor: PingCodeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, PCCredentials> {
    executor as PingCodeApiRequestExecutor.WithCredentialsAuth

    val credentials = acquireToken(indicator) //打开了浏览器
    executor.credentials = credentials // 授权成功后调用了这里 1.1

    val login = PCTokenCredentialsUi.acquireLogin(server, executor, indicator, isAccountUnique, null)
    return Pair(login, credentials)
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo = PCTokenCredentialsUi.handleError(error)

  override fun setBusy(busy: Boolean) = Unit

  override fun LayoutBuilder.centerPanel() {
    row {
      val progressLabel = JBLabel(message("label.login.progress")).apply {
        icon = AnimatedIcon.Default()
        foreground = getInactiveTextColor()
      }
      progressLabel()
    }
  }

  private fun acquireToken(indicator: ProgressIndicator): PCCredentials {
    // 触发 打开浏览器
    val credentialsFuture = PCOAuthService.instance.authorize()

    try {
      return ProgressIndicatorUtils.awaitWithCheckCanceled(credentialsFuture, indicator)
    }
    catch (pce: ProcessCanceledException) {
      credentialsFuture.completeExceptionally(pce)
      throw pce
    }
  }
}