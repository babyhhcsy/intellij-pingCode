// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.util.PingCodeCredentialsCreator
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.ui.UIUtil.getInactiveTextColor
import com.pingCode.api.PingCodeApiRequestExecutor
import com.pingCode.authentication.util.Validator
import javax.swing.JComponent

internal class PCRefreshCredentialsUi(
  val factory: PingCodeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : PCCredentialsUi() {

  private val LOG = logger<PCRefreshCredentialsUi>()

  private var fixedCredentials: PCCredentials = PCCredentials.EmptyCredentials

  override fun getPreferredFocusableComponent(): JComponent? = null

  override fun getValidator(): Validator = { null }

  override fun createExecutor(): PingCodeApiRequestExecutor = factory.create(fixedCredentials)

  override fun acquireLoginAndToken(
    server: PingCodeServerPath,
    executor: PingCodeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, PCCredentials> {
    LOG.info("fixedCredentials: ${fixedCredentials.accessToken}, ${fixedCredentials.refreshToken}, ${fixedCredentials.createdAt}")
    val credentials = PingCodeCredentialsCreator(server, executor, indicator).refresh(fixedCredentials.refreshToken)

    LOG.info("credentials: ${credentials.accessToken}, ${credentials.refreshToken}, ${credentials.createdAt}")
    executor as PingCodeApiRequestExecutor.WithCredentialsAuth
    executor.credentials = credentials

    val login = PCTokenCredentialsUi.acquireLogin(server, executor, indicator, isAccountUnique, null)
    LOG.warn("login: $login")
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

  fun setFixedCredentials(credentials: PCCredentials?) {
    if(credentials != null) {
      fixedCredentials = credentials
    }
  }
}