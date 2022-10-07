// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.authentication.accounts

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(name = "PingCodeAccounts", storages = [
  Storage(value = "pingCode.xml"),
  Storage(value = "pingCode_settings.xml", deprecated = true)
], reportStatistic = false)
internal class PCPersistentAccounts
  : AccountsRepository<PingCodeAccount>,
    PersistentStateComponent<Array<PingCodeAccount>> {

  private var state = emptyArray<PingCodeAccount>()

  override var accounts: Set<PingCodeAccount>
    get() = state.toSet()
    set(value) {
      state = value.toTypedArray()
    }

  override fun getState(): Array<PingCodeAccount> = state

  override fun loadState(state: Array<PingCodeAccount>) {
    this.state = state
  }
}