package com.pingCode.authentication.ui
import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.layout.LayoutBuilder
import com.pingCode.api.PingCodeApiRequestExecutor
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.accounts.PCAccountsUtils
import com.pingCode.authentication.util.errorOnEdt
import com.pingCode.authentication.util.submitIOTask
import com.pingCode.i18n.PingCodeBundle.message
import com.pingCode.ui.util.DialogValidationUtils.notBlank
internal typealias UniqueLoginPredicate = (login: String, server: PingCodeServerPath) -> Boolean

class PingCodeLoginPanel(
  executorFactory: PingCodeApiRequestExecutor.Factory,
  isAccountUnique: UniqueLoginPredicate
) : Wrapper() {

  private val serverTextField = ExtendableTextField(PingCodeServerPath.DEFAULT_HOST, 0)
  private var tokenAcquisitionError: ValidationInfo? = null

  private var clientIdTextField = JBTextField(PCAccountsUtils.APP_CLIENT_ID, 5)
  private var clientSecretTextField = JPasswordField(PCAccountsUtils.APP_CLIENT_SECRET, 5)

  private lateinit var currentUi: PCCredentialsUi

  // token登录

  private var oauthUi = PCOAuthCredentialsUi(executorFactory, isAccountUnique)
  // 刷新token验证
  private var refreshTokenUi = PCRefreshCredentialsUi(executorFactory, isAccountUnique)

  private val progressIcon = AnimatedIcon.Default()
  private val progressExtension = ExtendableTextComponent.Extension { progressIcon }

  var footer: LayoutBuilder.() -> Unit
    get() = oauthUi.footer
    set(value) {
      oauthUi.footer = value
      applyUi(currentUi)
    }

  init {
    applyUi(oauthUi)
  }

  private fun applyUi(ui: PCCredentialsUi) {
    currentUi = ui
    setContent(currentUi.getPanel())
    currentUi.getPreferredFocusableComponent()?.requestFocus()
    tokenAcquisitionError = null
  }

  fun getPreferredFocusableComponent(): JComponent? =
    serverTextField.takeIf { it.isEditable && it.text.isBlank() }
      ?: currentUi.getPreferredFocusableComponent()

  fun doValidateAll(): List<ValidationInfo> {
    val uiError =
      notBlank(serverTextField, message("credentials.server.cannot.be.empty"))
        ?: validateServerPath(serverTextField)
        ?: currentUi.getValidator().invoke()

    return listOfNotNull(uiError, tokenAcquisitionError)
  }

  private fun validateServerPath(field: JTextField): ValidationInfo? =
    try {
      PingCodeServerPath.from(field.text)
      null
    }
    catch (e: Exception) {
      ValidationInfo(message("credentials.server.path.invalid"), field)
    }

  private fun setBusy(busy: Boolean) {
    serverTextField.apply { if (busy) addExtension(progressExtension) else removeExtension(progressExtension) }
    serverTextField.isEnabled = !busy

    currentUi.setBusy(busy)
  }

  fun acquireLoginAndToken(progressIndicator: ProgressIndicator): CompletableFuture<Pair<String, PCCredentials>> {
    setBusy(true)
    tokenAcquisitionError = null

    val server = getServer()
    //PCOAuthCredentialsUi 授权登录ui PingCodeApiRequestExecutor
    val executor = currentUi.createExecutor()

    return service<ProgressManager>()
      .submitIOTask(progressIndicator) { currentUi.acquireLoginAndToken(server, executor, it) }
      .completionOnEdt(progressIndicator.modalityState) { setBusy(false) }
      .errorOnEdt(progressIndicator.modalityState) { setError(it) }
  }

  fun getServer(): PingCodeServerPath =
    PingCodeServerPath.from(serverTextField.text.trim(), clientIdTextField.text.trim(), String(clientSecretTextField.password))

  fun setServer(path: String, editable: Boolean = true) {
    serverTextField.apply {
      text = path
      isEditable = editable
    }

    clientIdTextField.isEditable = editable
    clientSecretTextField.isEditable = editable

    if (editable) {
      clientIdTextField.text = ""
      clientSecretTextField.text = ""
    }
  }

  fun setLogin(login: String?, editable: Boolean) {
//    passwordUi.setLogin(login.orEmpty(), editable)
//    oauthUi.setFixedLogin(if (editable) null else login)
  }
  fun setCredentials(credentials: PCCredentials?) {
    credentials ?.let {
      //oauthUi.setFixedCredentials(credentials)
      refreshTokenUi.setFixedCredentials(credentials)
    }
  }

  fun setError(exception: Throwable?) {
    tokenAcquisitionError = exception?.let {
      currentUi.handleAcquireError(it)
    }
  }

  fun setOAuthUi() = applyUi(oauthUi)

}