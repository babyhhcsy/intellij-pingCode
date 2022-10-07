package com.pingCode.authentication.util

import com.intellij.openapi.progress.ProgressIndicator
import com.pingCode.api.PingCodeApiRequestExecutor
import com.pingCode.api.PingCodeApiRequests
import com.pingCode.api.PingCodeServerPath
import com.pingCode.authentication.PCCredentials
import com.pingCode.authentication.accounts.PCAccountsUtils
import com.pingCode.exceptions.PingCodeStatusCodeException
import java.io.IOException

class PingCodeCredentialsCreator (private val server: PingCodeServerPath,
                                  private val executor: PingCodeApiRequestExecutor,
                                  private val indicator: ProgressIndicator
) {

    @Throws(IOException::class)
    fun create(login: String, password: CharArray): PCCredentials {
        return safeCreate(login, password)
    }

    @Throws(IOException::class)
    fun refresh(refreshToken: String): PCCredentials {
        return safeUpdate(refreshToken)
    }

    @Throws(IOException::class)
    private fun safeCreate(login: String, password: CharArray): PCCredentials {
        try {
            return executor.execute(indicator, PingCodeApiRequests.Auth.create(server, PCAccountsUtils.APP_CLIENT_SCOPE, login, password))
        } catch (e: PingCodeStatusCodeException) {
            e.setDetails("Can't create token: scopes - ${PCAccountsUtils.APP_CLIENT_SCOPE}")
            throw e
        }
    }

    @Throws(IOException::class)
    private fun safeUpdate(refreshToken: String): PCCredentials {
        try {
            return executor.execute(indicator, PingCodeApiRequests.Auth.update(server, refreshToken))
        } catch (e: PingCodeStatusCodeException) {
            e.setDetails("Can't update token: scopes - ${PCAccountsUtils.APP_CLIENT_SCOPE}")
            throw e
        }
    }
}