package com.example.data.model

import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val rule: AppRule? = null,
    val icon: Drawable? = null
)
