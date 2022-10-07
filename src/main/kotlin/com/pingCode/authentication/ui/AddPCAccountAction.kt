package com.pingCode.authentication.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.pingCode.i18n.PingCodeBundle.message
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent

class AddPCAccountAction : DumbAwareAction(){
    override fun actionPerformed(e: AnActionEvent) {

    }

}
//
//internal class PCAuthLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
////    BaseLoginDialog(project, parent, PingCodeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {
////
////    init {
////        title = message("login.to.pingCode")
////        loginPanel.setOAuthUi()
////        init()
////    }
////
////    override fun createActions(): Array<Action> = arrayOf(cancelAction)
////
////    override fun show() {
////        doOKAction()
////        super.show()
////    }
////
////    override fun createCenterPanel(): JComponent =
////        JBUI.Panels.simplePanel(loginPanel)
////            .withPreferredWidth(200)
////            .setPaddingCompensated()
//}