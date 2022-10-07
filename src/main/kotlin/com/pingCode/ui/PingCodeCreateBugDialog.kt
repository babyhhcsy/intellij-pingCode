package com.pingCode.ui

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.pingCode.authentication.accounts.PingCodeAccount
import javax.swing.JComponent

class PingCodeCreateBugDialog(project: Project,
                              accounts: Set<PingCodeAccount>,
                              defaultAccount: PingCodeAccount?,
                              fileName: String?,
                              secret: Boolean,
                              openInBrowser: Boolean,
                              copyLink: Boolean) : DialogWrapper(project,true),DataProvider {
    override fun createCenterPanel(): JComponent? {
        TODO("Not yet implemented")
    }

    override fun getData(dataId: String): Any? {
        TODO("Not yet implemented")
    }

}