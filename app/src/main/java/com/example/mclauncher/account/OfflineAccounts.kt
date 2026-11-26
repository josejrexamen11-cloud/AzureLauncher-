package com.example.mclauncher.account

import com.example.mclauncher.model.OfflineAccount
import java.util.UUID

object OfflineAccounts {
    /** Matches the UUID Minecraft derives for offline-mode players. */
    fun create(username: String): OfflineAccount {
        require(username.matches(Regex("^[A-Za-z0-9_]{3,16}$"))) {
            "Usernames must be 3–16 characters: letters, digits, underscore."
        }
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$username".toByteArray(Charsets.UTF_8))
        return OfflineAccount(username, uuid.toString())
    }
}
