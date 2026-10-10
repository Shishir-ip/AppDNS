package com.example

import com.example.data.model.AppRule
import com.example.data.model.DnsProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDefaultActionConstants() {
        assertEquals("REVERT_DEFAULT", AppRule.ACTION_REVERT_DEFAULT)
        assertEquals("TURN_OFF", AppRule.ACTION_TURN_OFF)
        assertEquals("KEEP_ACTIVE", AppRule.ACTION_KEEP_ACTIVE)
    }

    @Test
    fun testDnsProfileCreation() {
        val profile = DnsProfile(
            id = "cloudflare",
            name = "Cloudflare 1.1.1.1",
            description = "Standard",
            primaryIpv4 = "1.1.1.1",
            secondaryIpv4 = "1.0.0.1",
            category = "Privacy",
            isPreset = true
        )
        assertEquals("1.1.1.1", profile.primaryIpv4)
        assertEquals("1.0.0.1", profile.secondaryIpv4)
        assertTrue(profile.isPreset)
        assertEquals("Privacy", profile.category)
    }

    @Test
    fun testAppRuleCreation() {
        val rule = AppRule(
            packageName = "org.mozilla.firefox",
            appName = "Firefox",
            dnsProfileId = "adguard_dns",
            isEnabled = true,
            onExitAction = AppRule.ACTION_REVERT_DEFAULT
        )
        assertEquals("org.mozilla.firefox", rule.packageName)
        assertEquals("adguard_dns", rule.dnsProfileId)
        assertTrue(rule.isEnabled)
    }
}
