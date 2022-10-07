// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.ui

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.PingCodeAuthenticationManager
import com.pingCode.authentication.accounts.PingCodeAccount
import com.pingCode.authentication.ui.PCAccountsHost.Companion.createAddAccountLink
import com.pingCode.i18n.PingCodeBundle.message
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent
/**
 * 登录下拉选框组件
 * */
internal class PCAccountsComboBoxModel(accounts: Set<PingCodeAccount>, selection: PingCodeAccount?) :
  CollectionComboBoxModel<PingCodeAccount>(accounts.toMutableList(), selection),
  PCAccountsHost {

  override fun addAccount(server: PingCodeServerPath, login: String, credentials: PCCredentials) {
    val account = PingCodeAuthenticationManager.getInstance().registerAccount(login, server, credentials)

    add(account)
    selectedItem = account
  }

  override fun isAccountUnique(login: String, server: PingCodeServerPath): Boolean =
    PingCodeAuthenticationManager.getInstance().isAccountUnique(login, server)

  companion object {
    //下拉选择，Add Account 可以选择 用户密码登录或者其他
    fun Row.accountSelector(model: CollectionComboBoxModel<PingCodeAccount>, onChange: (() -> Unit)? = null) =
      cell {
        comboBox(model, { model.selected }, { })
          .constraints(pushX, growX)
          .withValidationOnApply { if (model.selected == null) error(message("dialog.message.account.cannot.be.empty")) else null }
          .applyToComponent {
            if (onChange != null) addActionListener { onChange() }
          }

        //if (model.size == 0) { // 判断用户是否登录，未登录显示 add Account 按钮
          createAddAccountLink()().withLargeLeftGap()
        //}
      }
//    fun accountSelector(@Nls label: String, model: CollectionComboBoxModel<PingCodeAccount>, onChange: (() -> Unit)? = null) = panel {
//      row(label) {
//        comboBox(model)
//          .horizontalAlign(HorizontalAlign.FILL)
//          .validationOnApply { if (model.selected == null) error(message("dialog.message.account.cannot.be.empty")) else null }
//          .applyToComponent { if (onChange != null) addActionListener { onChange() } }
//      }
//
//      if (model.size == 0) {
//        row {
//          cell(createAddAccountLink())
//        }
//      }
//    }
  }
}