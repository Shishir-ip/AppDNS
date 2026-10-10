package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val dnsProfileId: String,
    val isEnabled: Boolean = true,
    val onExitAction: String = ACTION_REVERT_DEFAULT,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACTION_REVERT_DEFAULT = "REVERT_DEFAULT"
        const val ACTION_TURN_OFF = "TURN_OFF"
        const val ACTION_KEEP_ACTIVE = "KEEP_ACTIVE"
    }
}
