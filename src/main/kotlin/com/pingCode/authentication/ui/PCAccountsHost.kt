// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.ui.components.DropDownLink
import javax.swing.JButton

internal interface PCAccountsHost {
  fun addAccount(server: PingCodeServerPath, login: String, credentials: PCCredentials)

  fun isAccountUnique(login: String, server: PingCodeServerPath): Boolean

  companion object {
    val KEY: DataKey<PCAccountsHost> = DataKey.create("PCAccountsHost")
    // 点击 Add Account 时触发
    fun createAddAccountLink(): JButton =
      DropDownLink(message("accounts.add.dropdown.link")) {
        val group = ActionManager.getInstance().getAction("PingCode.Accounts.AddAccount") as ActionGroup
        val dataContext = DataManager.getInstance().getDataContext(it)

        JBPopupFactory.getInstance().createActionGroupPopup(null, group, dataContext, ActionSelectionAid.MNEMONICS, false)
      }
  }
}