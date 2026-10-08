package com.blockit.domain.evaluator

import com.blockit.core.system.SystemAppResolver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleEvaluatorSafetyTest {

    private lateinit var evaluator: RuleEvaluator

    @Before
    fun setUp() {
        // Test with null systemAppResolver to ensure RuleEvaluator has infallible built-in self-protection
        evaluator = RuleEvaluator(systemAppResolver = null)
    }

    @Test
    fun selfPackage_isNeverBlocked_evenInWhitelistMode_withoutResolver() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Ultra Lock",
                whitelistPackages = setOf("com.allowed.app")
            )
        )

        assertFalse("com.blockit must NEVER be blocked", evaluator.isAppBlocked("com.blockit").isBlocked)
        assertFalse("com.blockit.debug must NEVER be blocked", evaluator.isAppBlocked("com.blockit.debug").isBlocked)
        assertFalse("com.blockit subpackage must NEVER be blocked", evaluator.isAppBlocked("com.blockit.ui.overlay").isBlocked)
    }

    @Test
    fun selfPackage_isNeverBlocked_inBlacklistMode() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = false,
                blockedPackages = mapOf("com.blockit" to "Malicious Rule", "com.blockit.debug" to "Malicious Rule")
            )
        )

        assertFalse(evaluator.isAppBlocked("com.blockit").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.blockit.debug").isBlocked)
    }

    @Test
    fun otherApps_areBlockedInWhitelistMode() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Lockdown",
                whitelistPackages = setOf("com.allowed.app")
            )
        )

        assertTrue(evaluator.isAppBlocked("com.unallowed.app").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.allowed.app").isBlocked)
    }
}
