// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.accounts

import com.fasterxml.jackson.databind.DeserializationFeature
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.accounts.PCAccountsUtils.jacksonMapper
import com.pingCode.util.PingCodeUtil
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.ApiStatus

internal val PingCodeAccount.isPCAccount: Boolean get() = server.isPingCodeDotCom()

/**
 * Handles application-level PingCode accounts
 */
@Service
internal class PCAccountManager
  : AccountManagerBase<PingCodeAccount, PCCredentials>(PingCodeUtil.SERVICE_DISPLAY_NAME) {

  override fun accountsRepository() = service<PCPersistentAccounts>()

  override fun serializeCredentials(credentials: PCCredentials): String =
    jacksonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials)

  override fun deserializeCredentials(credentials: String): PCCredentials {
    try {
      return jacksonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(credentials, PCCredentials::class.java)
    } catch (ignore: Exception) {
      return PCCredentials.EmptyCredentials
    }
  }

  init {
    @Suppress("DEPRECATION")
    addListener(this, object : AccountsListener<PingCodeAccount> {
      override fun onAccountListChanged(old: Collection<PingCodeAccount>, new: Collection<PingCodeAccount>) {
        val removedPublisher = ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_REMOVED_TOPIC)
        for (account in (old - new)) {
          removedPublisher.accountRemoved(account)
        }
        val tokenPublisher = ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANPCD_TOPIC)
        for (account in (new - old)) {
          tokenPublisher.tokenChanged(account)
        }
      }

      override fun onAccountCredentialsChanged(account: PingCodeAccount) =
        ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANPCD_TOPIC).tokenChanged(account)
    })
  }

  companion object {
    @Deprecated("Use TOPIC")
    @Suppress("DEPRECATION")
    @JvmStatic
    val ACCOUNT_REMOVED_TOPIC = Topic("GITEE_ACCOUNT_REMOVED", AccountRemovedListener::class.java)

    @Deprecated("Use TOPIC")
    @Suppress("DEPRECATION")
    @JvmStatic
    val ACCOUNT_TOKEN_CHANPCD_TOPIC = Topic("GITEE_ACCOUNT_TOKEN_CHANPCD", AccountTokenChangedListener::class.java)

    fun createAccount(name: String, server: PingCodeServerPath) = PingCodeAccount(name, server)
  }
}

@Deprecated("Use PingCodeAuthenticationManager.addListener")
@ApiStatus.ScheduledForRemoval
interface AccountRemovedListener {
  fun accountRemoved(removedAccount: PingCodeAccount)
}

@Deprecated("Use PingCodeAuthenticationManager.addListener")
@ApiStatus.ScheduledForRemoval
interface AccountTokenChangedListener {
  fun tokenChanged(account: PingCodeAccount)
}