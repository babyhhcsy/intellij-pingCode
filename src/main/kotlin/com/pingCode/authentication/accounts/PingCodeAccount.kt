package com.pingCode.authentication.accounts

import com.intellij.collaboration.auth.Account
import com.intellij.collaboration.auth.ServerAccount
import com.intellij.configurationStore.Property
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Transient
import com.pingCode.api.PingCodeServerPath
import org.jetbrains.annotations.VisibleForTesting
@Tag("account")
class PingCodeAccount(
    @set:Transient
    @NlsSafe
    @Attribute("name")
    override var name: String = "",

    @com.intellij.util.xmlb.annotations.Property(style = com.intellij.util.xmlb.annotations.Property.Style.ATTRIBUTE, surroundWithTag = false)
    override val server: PingCodeServerPath = PingCodeServerPath(),

    @Attribute("id")
    @VisibleForTesting
    override val id: String = Account.generateId()
): ServerAccount(){
    override fun toString(): String = "$server/$name"
}