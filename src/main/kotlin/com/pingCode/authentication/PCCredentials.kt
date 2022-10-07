package com.pingCode.authentication

import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh
import com.pingCode.authentication.accounts.PCAccountsUtils

class PCCredentials (
    override val accessToken: String,
    override val refreshToken: String,
    override val expiresIn: Long,
    val tokenType: String,
    val scope: String,
    val createdAt: Long
) : CredentialsWithRefresh {

    override fun isAccessTokenValid(): Boolean {
        return System.currentTimeMillis() < (createdAt + expiresIn) * 1000
    }

    companion object {
        private const val empty_str = ""
        val EmptyCredentials = PCCredentials(empty_str, empty_str, 0, empty_str, empty_str, 0)

        fun createCredentials(accessToken: String, refreshToken: String) =
            PCCredentials(accessToken, refreshToken, 86400, "bearer", PCAccountsUtils.APP_CLIENT_SCOPE, System.currentTimeMillis() / 1000)
    }
}