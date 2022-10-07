package com.pingCode.authentication.ui

import com.pingCode.api.PingCodeApiRequests
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.accounts.PCAccountsUtils
import com.pingCode.exceptions.PingCodeAuthenticationException
import com.pingCode.exceptions.PingCodeParseException
import com.pingCode.i18n.PingCodeBundle.message
import com.pingCode.ui.util.DialogValidationUtils.notBlank
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.layout.LayoutBuilder
import com.pingCode.api.PingCodeApiRequestExecutor
import com.pingCode.authentication.util.Validator
import java.net.UnknownHostException
import javax.swing.JComponent

internal class PCTokenCredentialsUi(
  private val serverTextField: ExtendableTextField,
  val factory: PingCodeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : PCCredentialsUi() {

  private val accessTokenTextField = JBTextField()
  private val refreshTokenTextField = JBTextField()

  private var fixedLogin: String? = null
  private var fixedCredentials: PCCredentials? = null

  fun setAccessToken(token: String) {
    accessTokenTextField.text = token
  }

  fun setRefreshToken(token: String) {
    refreshTokenTextField.text = token
  }

  override fun LayoutBuilder.centerPanel() {
    row(message("credentials.server.field")) {
      serverTextField(pushX, growX)
    }
    row(message("credentials.access.token.field")) {
      cell {
        accessTokenTextField(
          comment = message("login.insufficient.scopes", PCAccountsUtils.APP_CLIENT_SCOPE),
          constraints = arrayOf(pushX, growX)
        )
      }
    }
    row(message("credentials.refresh.token.field")) {
      cell {
        refreshTokenTextField(
          comment = message("login.insufficient.scopes", PCAccountsUtils.APP_CLIENT_SCOPE),
          constraints = arrayOf(pushX, growX)
        )
      }
    }
  }

  override fun getPreferredFocusableComponent(): JComponent = accessTokenTextField

  override fun getValidator(): Validator = {
    notBlank(accessTokenTextField, message("login.token.cannot.be.empty")) ?: notBlank(refreshTokenTextField, message("login.token.cannot.be.empty"))
  }

  override fun createExecutor() = factory.create(accessTokenTextField.text)

  override fun acquireLoginAndToken(
    server: PingCodeServerPath,
    executor: PingCodeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, PCCredentials> {

    val login = acquireLogin(server, executor, indicator, isAccountUnique, fixedLogin)

    return Pair(login, fixedCredentials ?: PCCredentials.createCredentials(accessTokenTextField.text, refreshTokenTextField.text))
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo =
    when (error) {
      is PingCodeParseException -> ValidationInfo(error.message ?: message("credentials.invalid.server.path"), serverTextField)
      else -> handleError(error)
    }

  override fun setBusy(busy: Boolean) {
    accessTokenTextField.isEnabled = !busy
  }

  fun setFixedLogin(fixedLogin: String?) {
    this.fixedLogin = fixedLogin
  }

  fun setFixedCredentials(credentials: PCCredentials?) {
    fixedCredentials = credentials

    credentials ?.let {
      setAccessToken(it.accessToken)
      setRefreshToken(it.refreshToken)
    }
  }

  companion object {
    fun acquireLogin(
      server: PingCodeServerPath,
      executor: PingCodeApiRequestExecutor,
      indicator: ProgressIndicator,
      isAccountUnique: UniqueLoginPredicate,
      fixedLogin: String?
    ): String {
      //1.3 授权调用更改授权信息
      val login = executor.execute(indicator, PingCodeApiRequests.CurrentUser.get(server)).login

      if (fixedLogin != null && fixedLogin != login) throw PingCodeAuthenticationException("Token should match username \"$fixedLogin\"")
      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

      return login
    }

    fun handleError(error: Throwable): ValidationInfo =
      when (error) {
        is LoginNotUniqueException -> ValidationInfo(message("login.account.already.added", error.login)).withOKEnabled()
        is UnknownHostException -> ValidationInfo(message("server.unreachable")).withOKEnabled()
        is PingCodeAuthenticationException -> ValidationInfo(message("credentials.incorrect", error.message.orEmpty())).withOKEnabled()
        else -> ValidationInfo(message("credentials.invalid.auth.data", error.message.orEmpty())).withOKEnabled()
      }
  }

}