package com.blockit.domain.evaluator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleEvaluatorTest {

    private lateinit var evaluator: RuleEvaluator

    @Before
    fun setUp() {
        evaluator = RuleEvaluator()
    }

    @Test
    fun isAppBlocked_blacklistMode_blocksMatchingPackage() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                blockedPackages = mapOf("com.instagram.android" to "Social Media Lock"),
                blockedDomains = emptyMap(),
                hasActiveWhitelist = false
            )
        )

        val blockedResult = evaluator.isAppBlocked("com.instagram.android")
        assertTrue(blockedResult.isBlocked)
        assertEquals("com.instagram.android", blockedResult.matchedTarget)
        assertEquals("Social Media Lock", blockedResult.profileName)
        assertFalse(blockedResult.isWhitelistBlock)

        val unblockedResult = evaluator.isAppBlocked("com.google.android.apps.messaging")
        assertFalse(unblockedResult.isBlocked)
    }

    @Test
    fun isAppBlocked_whitelistMode_permitsOnlyWhitelistedPackages() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Study Kiosk",
                whitelistPackages = setOf("com.sec.android.app.popupcalculator", "com.oxford.dictionary")
            )
        )

        // Non-whitelisted app is blocked and marked as isWhitelistBlock
        val nonWhitelisted = evaluator.isAppBlocked("com.twitter.android")
        assertTrue(nonWhitelisted.isBlocked)
        assertTrue(nonWhitelisted.isWhitelistBlock)
        assertEquals("Study Kiosk", nonWhitelisted.profileName)
        assertTrue(nonWhitelisted.allowedApps.contains("com.oxford.dictionary"))

        // Whitelisted app is allowed
        val allowedApp = evaluator.isAppBlocked("com.oxford.dictionary")
        assertFalse(allowedApp.isBlocked)

        val allowedCalc = evaluator.isAppBlocked("com.sec.android.app.popupcalculator")
        assertFalse(allowedCalc.isBlocked)
    }

    @Test
    fun isDomainBlocked_matchesDomainsAndSubdomains() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                blockedDomains = mapOf(
                    "youtube.com" to "Distraction Free",
                    "*.reddit.com" to "Focus Mode"
                )
            )
        )

        // Direct domain match
        val yt = evaluator.isDomainBlocked("youtube.com")
        assertTrue(yt.isBlocked)
        assertEquals("Distraction Free", yt.profileName)

        // Subdomain match
        val mobileYt = evaluator.isDomainBlocked("https://m.youtube.com/watch?v=123")
        assertTrue(mobileYt.isBlocked)

        // Wildcard subdomain
        val reddit = evaluator.isDomainBlocked("old.reddit.com")
        assertTrue(reddit.isBlocked)
        assertEquals("Focus Mode", reddit.profileName)

        // Unblocked domain
        val github = evaluator.isDomainBlocked("https://github.com/project")
        assertFalse(github.isBlocked)
    }
}
