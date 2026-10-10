package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dns_profiles")
data class DnsProfile(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val primaryIpv4: String,
    val secondaryIpv4: String = "",
    val primaryIpv6: String = "",
    val dohUrl: String = "",
    val dotHost: String = "",
    val category: String = "Privacy",
    val isPreset: Boolean = false,
    val isFavorite: Boolean = false,
    val accentColorHex: String = "#00E5FF",
    val latencyMs: Long = -1L
)
