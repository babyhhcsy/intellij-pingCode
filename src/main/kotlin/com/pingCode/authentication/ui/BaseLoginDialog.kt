// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui


import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.errorOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.pingCode.api.PingCodeApiRequestExecutor
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import java.awt.Component
import javax.swing.JComponent

internal fun JComponent.setPaddingCompensated(): JComponent =
  apply { putClientProperty(IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY, false) }

internal abstract class BaseLoginDialog(
  project: Project?,
  parent: Component?,
  executorFactory: PingCodeApiRequestExecutor.Factory,
  isAccountUnique: UniqueLoginPredicate
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {

  protected val loginPanel = PingCodeLoginPanel(executorFactory, isAccountUnique)

  private var _login = ""
  private var _credentials = PCCredentials.EmptyCredentials

  val login: String get() = _login
  val credentials: PCCredentials get() = _credentials

  val server: PingCodeServerPath get() = loginPanel.getServer()

  fun setLogin(login: String?, editable: Boolean) = loginPanel.setLogin(login, editable)

  fun setCredentials(credentials: PCCredentials?) = loginPanel.setCredentials(credentials)

  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  fun setError(exception: Throwable) {
    loginPanel.setError(exception)
    startTrackingValidation()
  }

  override fun getPreferredFocusedComponent(): JComponent? = loginPanel.getPreferredFocusableComponent()

  override fun doValidateAll(): List<ValidationInfo> = loginPanel.doValidateAll()
  // 通用接口点击ok是执行
  override fun doOKAction() {
    val modalityState = ModalityState.stateForComponent(loginPanel)
    //https://jetbrains.design/intellij/controls/progress_indicators/ 空的进度条
    val emptyProgressIndicator = EmptyProgressIndicator(modalityState)
    Disposer.register(disposable) { emptyProgressIndicator.cancel() }
    // 登录窗口授权成功后，调用successOnEdt 方法
    loginPanel.acquireLoginAndToken(emptyProgressIndicator)
      .successOnEdt(modalityState) { (login, credentials) ->

        _login = login // thero
        _credentials = credentials

        close(OK_EXIT_CODE, true)
      }
      .errorOnEdt(modalityState) {
        if (!CompletableFutureUtil.isCancellation(it)) startTrackingValidation()
      }
  }
}