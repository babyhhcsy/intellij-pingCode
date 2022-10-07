package com.pingCode.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.refactoring.rename.api.FileOperation;
import com.pingCode.authentication.ui.AddPCAccountAction;
import com.pingCode.i18n.PingCodeBundle;
import com.pingCode.icons.PingCodeIcons;
import com.pingCode.util.PingCodeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class PingCodeCreateBugAction extends DumbAwareAction {
    public static final Logger LOGGER = PingCodeUtil.LOG;

    protected PingCodeCreateBugAction(){
        super(PingCodeBundle.messagePointer("ping.create.bug.title"),
                PingCodeBundle.messagePointer("ping.create.bug.desc"),
                PingCodeIcons.pingCode_Add_Bug_icon);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        LOGGER.warn("123");
        AddPCAccountAction addPCAccountAction = new AddPCAccountAction();
        addPCAccountAction.actionPerformed(e);
        LOGGER.warn("456");
    }
}
