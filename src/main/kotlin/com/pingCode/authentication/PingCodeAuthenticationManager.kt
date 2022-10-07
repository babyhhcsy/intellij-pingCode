/*
 *  Copyright 2016-2019 码云 - PingCode
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.pingCode.authentication

import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.accounts.PCAccountManager
import com.pingCode.authentication.accounts.PingCodeAccount
import com.pingCode.authentication.accounts.PingCodeProjectDefaultAccountHolder
import com.pingCode.i18n.PingCodeBundle
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.pingCode.api.PCLoginRequest
import com.pingCode.api.loginWithOAuthOrTokens
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.TestOnly
import java.awt.Component

internal class PCAccountAuthData(val account: PingCodeAccount, login: String, val credentials: PCCredentials) : AuthData(login, credentials.accessToken) {
  val server: PingCodeServerPath get() = account.server
}

/**
 * Entry point for interactions with PingCode authentication subsystem
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/PingCodeAuthenticationManager.kt
 * @author JetBrains s.r.o.
 */
class PingCodeAuthenticationManager internal constructor() {

  private val accountManager: PCAccountManager
    get() = service()

  @CalledInAny
  fun hasAccounts(): Boolean = accountManager.accounts.isNotEmpty()

  @CalledInAny
  fun getAccounts(): Set<PingCodeAccount> = accountManager.accounts

  // 查找证书信息
  @CalledInAny
  internal fun getCredentialsForAccount(account: PingCodeAccount): PCCredentials? =
    accountManager.findCredentials(account)

  @RequiresEdt
  @JvmOverloads
  internal fun requestUpdateCredentials(account: PingCodeAccount, expiredCredentials: PCCredentials, project: Project?, parentComponent: Component? = null): PCCredentials? =
    login(
      project, parentComponent,
      PCLoginRequest(
        text = PingCodeBundle.message("account.credentials.update.for", account),
        server = account.server, login = account.name,
        credentials = expiredCredentials, isCheckLoginUnique = true
      )
    )?.updateAccount(account)

  @RequiresEdt
  @JvmOverloads
  internal fun requestNewCredentials(account: PingCodeAccount, project: Project?, parentComponent: Component? = null): PCCredentials? =
    login(
      project, parentComponent,
      PCLoginRequest(
        text = PingCodeBundle.message("account.credentials.missing.for", account),
        server = account.server, login = account.name
      )
    )?.updateAccount(account)

  @RequiresEdt
  @JvmOverloads
  fun requestNewAccount(project: Project?, parentComponent: Component? = null): PingCodeAccount? =
    login(
      project, parentComponent,
      PCLoginRequest(isCheckLoginUnique = true)
    )?.registerAccount()

  @RequiresEdt
  @JvmOverloads
  fun requestNewAccountForServer(server: PingCodeServerPath, project: Project?, parentComponent: Component? = null): PingCodeAccount? =
    login(
      project, parentComponent,
      PCLoginRequest(server = server, isCheckLoginUnique = true)
    )?.registerAccount()

  @RequiresEdt
  @JvmOverloads
  fun requestNewAccountForServer(server: PingCodeServerPath, login: String, project: Project?, parentComponent: Component? = null): PingCodeAccount? =
    login(
      project, parentComponent,
      PCLoginRequest(server = server, login = login, isLoginEditable = false, isCheckLoginUnique = true)
    )?.registerAccount()

//  @RequiresEdt
//  fun requestNewAccountForDefaultServer(project: Project?, useToken: Boolean = false): PingCodeAccount? {
//    return PCLoginRequest(server = PingCodeServerPath.DEFAULT_SERVER, isCheckLoginUnique = true).let {
//      if (!useToken) it.loginWithPassword(project, null) else it.loginWithTokens(project, null)
//    }?.registerAccount()
//  }

  internal fun isAccountUnique(name: String, server: PingCodeServerPath) =
    accountManager.accounts.none { it.name == name && it.server == server }

  @RequiresEdt
  @JvmOverloads
  fun requestReLogin(account: PingCodeAccount, project: Project?, parentComponent: Component? = null): Boolean =
    login(
      project, parentComponent,
      PCLoginRequest(server = account.server, login = account.name)
    )?.updateAccount(account) != null

  @RequiresEdt
  internal fun login(project: Project?, parentComponent: Component?, request: PCLoginRequest): PCAccountAuthData? =
    request.loginWithOAuthOrTokens(project, parentComponent)

  @RequiresEdt
  internal fun removeAccount(account: PingCodeAccount) {
    accountManager.removeAccount(account)
  }

  @RequiresEdt
  internal fun updateAccountCredentials(account: PingCodeAccount, newCredentials: PCCredentials) {
    accountManager.updateAccount(account, newCredentials)
  }

  @RequiresEdt
  internal fun registerAccount(name: String, server: PingCodeServerPath, credentials: PCCredentials): PingCodeAccount =
    registerAccount(PCAccountManager.createAccount(name, server), credentials)

  @RequiresEdt
  internal fun registerAccount(account: PingCodeAccount, credentials: PCCredentials): PingCodeAccount {
    accountManager.updateAccount(account, credentials)
    return account
  }

  @TestOnly
  fun clearAccounts() {
    accountManager.updateAccounts(emptyMap())
  }

  fun getDefaultAccount(project: Project): PingCodeAccount? =
    project.service<PingCodeProjectDefaultAccountHolder>().account

  @TestOnly
  fun setDefaultAccount(project: Project, account: PingCodeAccount?) {
    project.service<PingCodeProjectDefaultAccountHolder>().account = account
  }

  @RequiresEdt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean =
    hasAccounts() || requestNewAccount(project, parentComponent) != null

  fun getSingleOrDefaultAccount(project: Project): PingCodeAccount? =
    project.service<PingCodeProjectDefaultAccountHolder>().account
      ?: accountManager.accounts.singleOrNull()

  @RequiresEdt
  fun addListener(disposable: Disposable, listener: AccountsListener<PingCodeAccount>) =
    accountManager.addListener(disposable, listener)

  companion object {
    @JvmStatic
    fun getInstance(): PingCodeAuthenticationManager {
      return service()
    }
  }
}

private fun PCAccountAuthData.registerAccount(): PingCodeAccount =
  PingCodeAuthenticationManager.getInstance().registerAccount(login, server, credentials)

private fun PCAccountAuthData.updateAccount(account: PingCodeAccount): PCCredentials {
  account.name = login
  PingCodeAuthenticationManager.getInstance().updateAccountCredentials(account, credentials)
  return credentials
}
