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
package com.pingCode.authentication.accounts

import com.pingCode.i18n.PingCodeBundle
 import com.pingCode.util.PingCodeUtil
import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import com.pingCode.authentication.util.PingCodeNotificationIdsHolder
import com.pingCode.util.PingCodeNotifications

/**
 * Handles default PingCode account for project
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GithubProjectDefaultAccountHolder.kt
 * @author JetBrains s.r.o.
 */
@Suppress("UNCHECKED_CAST")
@State(name = "PingCodeDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
internal class PingCodeProjectDefaultAccountHolder(project: Project)
  : PersistentDefaultAccountHolder<PingCodeAccount>(project) {

  override fun accountManager() = service<PCAccountManager>()

  override fun notifyDefaultAccountMissing() = runInEdt {
    val title = PingCodeBundle.message("accounts.default.missing")

    PingCodeUtil.LOG.info("${title}; ${""}")
    VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.createNotification(title, NotificationType.WARNING)
      .setDisplayId(PingCodeNotificationIdsHolder.MISSING_DEFAULT_ACCOUNT)
      .addAction(PingCodeNotifications.getConfigureAction(project))
      .notify(project)
  }
}
