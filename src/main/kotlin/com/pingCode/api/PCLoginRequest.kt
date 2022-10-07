// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.api

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.ui.*
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.pingCode.authentication.PCAccountAuthData
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.PingCodeAuthenticationManager
import com.pingCode.authentication.accounts.PCAccountManager.Companion.createAccount
import git4idea.DialogManager
import java.awt.Component

internal class PCLoginRequest(
  @NlsContexts.DialogMessage
  val text: String? = null,
  val error: Throwable? = null,

  val server: PingCodeServerPath? = null,
  val isServerEditable: Boolean = server == null,

  val login: String? = null,
  val isLoginEditable: Boolean = true,
  val isCheckLoginUnique: Boolean = false,

  val token: String? = null,
  val credentials: PCCredentials? = null
)

internal fun PCLoginRequest.loginRefreshTokens(project: Project?, parentComponent: Component?): PCAccountAuthData? {
  val dialog = PCRefreshTokensLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}
internal fun PCLoginRequest.loginWithOAuthOrTokens(project: Project?, parentComponent: Component?): PCAccountAuthData? =
  when (promptOAuthLogin(this, project, parentComponent)) {
    com.intellij.openapi.ui.Messages.YES ->
      loginWithOAuth(project, parentComponent)
    else ->
      null
  }
internal fun PCLoginRequest.loginWithOAuth(project: Project?, parentComponent: Component?): PCAccountAuthData? {
  val dialog = PCOAuthLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}

private val PCLoginRequest.isLoginUniqueChecker: UniqueLoginPredicate
  get() = { login, server -> !isCheckLoginUnique || PingCodeAuthenticationManager.getInstance().isAccountUnique(login, server) }

private fun PCLoginRequest.configure(dialog: BaseLoginDialog) {
  error?.let { dialog.setError(it) }
  server?.let { dialog.setServer(it.toString(), isServerEditable) }
  login?.let { dialog.setLogin(it, isLoginEditable) }
//  token?.let { dialog.setToken(it) }
  credentials?.let { dialog.setCredentials(it) }
}

private fun BaseLoginDialog.getAuthData(): PCAccountAuthData? {
  DialogManager.show(this)
  return if (isOK) PCAccountAuthData(createAccount(login, server), login, credentials) else null
}

private fun promptOAuthLogin(request: PCLoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(message("login.to.pingCode"), request.text
    ?: message("dialog.message.login.to.continue"))
    .yesText(message("login.via.pingCode.action"))
    .noText(message("button.use.other"))
    .icon(Messages.getWarningIcon())

  if (parentComponent != null) {
    return builder.show(parentComponent)
  } else {
    return builder.show(project)
  }
}

private fun promptTokenLogin(request: PCLoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(message("login.to.pingCode"), request.text?: message("dialog.message.login.to.continue"))
    .yesText(message("button.use.password"))
    .noText(message("button.use.tokens"))
    .icon(Messages.getWarningIcon())

  if (parentComponent != null) {
    return builder.show(parentComponent)
  } else {
    return builder.show(project)
  }
}