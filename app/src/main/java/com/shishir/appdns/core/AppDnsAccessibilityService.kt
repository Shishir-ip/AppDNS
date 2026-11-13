package com.shishir.appdns.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal, fast accessibility listener. Only window-state events are
 * subscribed (no content retrieval, no view ids) and notificationTimeout is 0
 * so events are delivered immediately — the v2 config delayed them by 100 ms.
 */
class AppDnsAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            serviceInfo = (serviceInfo ?: AccessibilityServiceInfo()).apply {
                eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                notificationTimeout = 0
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            }
        } catch (_: Exception) {
        }
        DnsEngine.get(this).onServiceConnected(this, true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        DnsEngine.get(this).onForeground(pkg)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        DnsEngine.get(this).onServiceConnected(null, false)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        DnsEngine.get(this).onServiceConnected(null, false)
        super.onDestroy()
    }
}