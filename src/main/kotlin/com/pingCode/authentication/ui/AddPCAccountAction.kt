package com.pingCode.authentication.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.pingCode.api.PingCodeApiRequestExecutor
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PingCodeAuthenticationManager
import com.pingCode.i18n.PingCodeBundle.message
import com.pingCode.util.PingCodeSettings
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent

class AddPCAccountAction : DumbAwareAction(){

    override fun actionPerformed(e: AnActionEvent) {

        // gitee 授权管理 实例
        val authManager: PingCodeAuthenticationManager = PingCodeAuthenticationManager.getInstance()
        // gitee 设置
        val settings: PingCodeSettings = PingCodeSettings.getInstance()
        // 获得当前 project 对象
        val project = e.getData(CommonDataKeys.PROJECT)
        if (project == null || project.isDefault) {
            return
        }
        var accountModel = PCAccountsComboBoxModel(authManager.getAccounts(), authManager.getDefaultAccount(project) ?: authManager.getAccounts().firstOrNull())

        val dialog = PCOAuthLoginDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountModel::isAccountUnique)
        dialog.setServer(PingCodeServerPath.DEFAULT_HOST, false)

        if (dialog.showAndGet()) {
            accountModel.addAccount(dialog.server, dialog.login, dialog.credentials)
        }
    }

}

internal class PCOAuthLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
    BaseLoginDialog(project, parent, PingCodeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

    init {
        title = message("login.to.pingCode")
        loginPanel.setOAuthUi()
        init()
    }

    override fun createActions(): Array<Action> = arrayOf(cancelAction)

    override fun show() {
        doOKAction()
        super.show()
    }

    override fun createCenterPanel(): JComponent =
        JBUI.Panels.simplePanel(loginPanel)
            .withPreferredWidth(200)
            .setPaddingCompensated()
}