// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.pingCode.api

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.UpdateInBackground
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.NlsSafe

@Service
class PCRequestExecutorBreaker {

  @Volatile
  var isRequestsShouldFail = false

  class Action : ToggleAction(actionText), DumbAware, UpdateInBackground {
    override fun isSelected(e: AnActionEvent) =
      service<PCRequestExecutorBreaker>().isRequestsShouldFail


    override fun setSelected(e: AnActionEvent, state: Boolean) {
      service<PCRequestExecutorBreaker>().isRequestsShouldFail = state
    }

    companion object {
      @NlsSafe
      private val actionText = "Break Gitee API Requests"
    }
  }
}