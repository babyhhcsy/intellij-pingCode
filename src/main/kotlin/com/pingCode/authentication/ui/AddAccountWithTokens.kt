// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui

import com.pingCode.api.PingCodeServerPath
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.pingCode.api.PingCodeApiRequestExecutor
import git4idea.i18n.GitBundle
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent

class AddPCAccountWithTokensAction : BaseAddAccountWithTokensAction() {
  override val defaultServer: String get() = PingCodeServerPath.DEFAULT_HOST
}

class AddPCEAccountAction : BaseAddAccountWithTokensAction() {
  override val defaultServer: String get() = ""
}

abstract class BaseAddAccountWithTokensAction : DumbAwareAction() {
  abstract val defaultServer: String

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(PCAccountsHost.KEY) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val accountsHost = e.getData(PCAccountsHost.KEY)!!
    val dialog = newAddAccountDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountsHost::isAccountUnique)

    dialog.setServer(defaultServer, defaultServer != PingCodeServerPath.DEFAULT_HOST)
    if (dialog.showAndGet()) {
      accountsHost.addAccount(dialog.server, dialog.login, dialog.credentials)
    }
  }
}

private fun newAddAccountDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate): BaseLoginDialog =
  PCTokensLoginDialog(project, parent, isAccountUnique).apply {
    title = message("dialog.title.add.pingCode.account")
    setLoginButtonText(message("accounts.add.button"))
  }

internal class PCTokensLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, PingCodeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.pingCode")
    setLoginButtonText(GitBundle.message("login.dialog.button.login"))
    //loginPanel.setTokenUi()

    init()
  }

  internal fun setLoginButtonText(@NlsContexts.Button text: String) = setOKButtonText(text)

  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()
}

internal class PCRefreshTokensLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, PingCodeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.pingCode")
    //loginPanel.setRefreshTokenUi()
    init()
  }

  override fun createActions(): Array<Action> = arrayOf(cancelAction)

  override fun show() {
    doOKAction()
    super.show()
  }

  override fun createCenterPanel(): JComponent =
    simplePanel(loginPanel)
      .withPreferredWidth(200)
      .setPaddingCompensated()
}