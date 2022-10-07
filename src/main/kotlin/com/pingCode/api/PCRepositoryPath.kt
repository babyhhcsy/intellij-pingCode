package com.pingCode.api

import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil

class PCRepositoryPath(val owner: String, val repository: String) {
    fun toString(showOwner: Boolean) = if (showOwner) "$owner/$repository" else repository

    @NlsSafe
    override fun toString() = "$owner/$repository"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PCRepositoryPath) return false

        if (!owner.equals(other.owner, true)) return false
        if (!repository.equals(other.repository, true)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = StringUtil.stringHashCodeInsensitive(owner)
        result = 31 * result + StringUtil.stringHashCodeInsensitive(repository)
        return result
    }
}