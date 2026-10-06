package com.blockit.domain.evaluator

import com.blockit.core.system.SystemAppResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSystemAppResolver(
    private val permitted: Set<String> = setOf(
        "com.blockit",
        "com.android.launcher3",
        "com.google.android.dialer",
        "com.google.android.inputmethod.latin",
        "com.android.systemui",
        "com.google.android.permissioncontroller"
    )
) : SystemAppResolver {
    private var userAllowed = mutableSetOf<String>()

    override fun isAlwaysPermitted(packageName: String, isWhitelistMode: Boolean): Boolean {
        if (packageName == "com.android.launcher3") {
            return !isWhitelistMode // Home launcher blocked during whitelist to enforce Focus Home
        }
        return permitted.contains(packageName) || userAllowed.contains(packageName)
    }

    override fun isSystemCritical(packageName: String): Boolean {
        return packageName != "com.android.launcher3" && permitted.contains(packageName)
    }

    override fun getAlwaysPermittedPackages(): Set<String> = permitted + userAllowed
    override fun getHomePackages(): Set<String> = setOf("com.android.launcher3")
    override fun getDefaultDialerPackage(): String = "com.google.android.dialer"
    override fun getUserAlwaysAllowedPackages(): Set<String> = userAllowed
    override fun setUserAlwaysAllowedPackages(packages: Set<String>) {
        userAllowed = packages.toMutableSet()
    }
}

class RuleEvaluatorTest {

    private lateinit var evaluator: RuleEvaluator
    private lateinit var fakeSystemResolver: FakeSystemAppResolver

    @Before
    fun setUp() {
        fakeSystemResolver = FakeSystemAppResolver()
        evaluator = RuleEvaluator(systemAppResolver = fakeSystemResolver)
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

        // Normal launcher is allowed in blacklist mode
        assertFalse(evaluator.isAppBlocked("com.android.launcher3").isBlocked)
    }

    @Test
    fun isAppBlocked_whitelistMode_permitsOnlyWhitelistedPackagesAndSystemCritical() {
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

        // Whitelisted apps are allowed
        val allowedApp = evaluator.isAppBlocked("com.oxford.dictionary")
        assertFalse(allowedApp.isBlocked)

        val allowedCalc = evaluator.isAppBlocked("com.sec.android.app.popupcalculator")
        assertFalse(allowedCalc.isBlocked)

        // Third-party home launcher is BLOCKED in Whitelist Mode so user stays in Focus Home
        val launcherResult = evaluator.isAppBlocked("com.android.launcher3")
        assertTrue("Launcher must be blocked in Whitelist mode to hold Focus Home kiosk", launcherResult.isBlocked)

        // System critical apps (dialer, keyboard, system UI, permission dialog, FocusShield) are NEVER blocked
        assertFalse(evaluator.isAppBlocked("com.google.android.dialer").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.google.android.inputmethod.latin").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.android.systemui").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.google.android.permissioncontroller").isBlocked)
        assertFalse(evaluator.isAppBlocked("com.blockit").isBlocked)
    }

    @Test
    fun isAppBlocked_evaluationLatency_underOneSecondTarget() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Lockdown",
                whitelistPackages = (1..500).map { "com.app.test$it" }.toSet()
            )
        )

        val start = System.nanoTime()
        val result = evaluator.isAppBlocked("com.app.test250")
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        assertFalse(result.isBlocked)
        assertTrue("Rule lookup must be instantaneous (< 10ms)", elapsedMs < 10.0)
    }

    @Test
    fun isAppBlocked_whitelistMode_everyAppInPermittedListNeverBlocked() {
        val permittedList = setOf("com.anikku.app", "com.bolt.client", "com.aniyomi.app", "com.notes.app")
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Allowed Daily Apps",
                whitelistPackages = permittedList
            )
        )

        for (pkg in permittedList) {
            val eval = evaluator.isAppBlocked(pkg)
            assertFalse("App $pkg in permitted list must NOT be blocked", eval.isBlocked)
        }

        // Unpermitted app MUST be blocked
        val unpermitted = evaluator.isAppBlocked("com.facebook.katana")
        assertTrue(unpermitted.isBlocked)
    }

    @Test
    fun isAppBlocked_blankOrEmptyPackage_neverBlocks() {
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Strict Kiosk",
                whitelistPackages = setOf("com.allowed.app")
            )
        )

        assertFalse(evaluator.isAppBlocked("").isBlocked)
        assertFalse(evaluator.isAppBlocked("   ").isBlocked)
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

    @Test
    fun testReproduction_userScenario_anikku_bolt_aniyomi() {
        val targetApps = listOf("com.anikku.app", "bolt.delivery.client", "com.aniyomi.app")

        // 1. Blacklist active session with Bolt added
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                blockedPackages = mapOf("bolt.delivery.client" to "Work Focus"),
                blockedDomains = emptyMap(),
                hasActiveWhitelist = false
            )
        )

        assertTrue("Bolt must be blocked in blacklist mode", evaluator.isAppBlocked("bolt.delivery.client").isBlocked)
        assertFalse("Anikku not in blacklist must not be blocked", evaluator.isAppBlocked("com.anikku.app").isBlocked)
        assertFalse("Aniyomi not in blacklist must not be blocked", evaluator.isAppBlocked("com.aniyomi.app").isBlocked)

        // 2. Whitelist active session with all three permitted
        evaluator.setSnapshot(
            ActiveRuleSnapshot(
                hasActiveWhitelist = true,
                whitelistProfileName = "Allowed Productivity",
                whitelistPackages = targetApps.toSet()
            )
        )

        for (app in targetApps) {
            val res = evaluator.isAppBlocked(app)
            assertFalse("Permitted app $app must NOT be blocked in whitelist mode", res.isBlocked)
        }

        val distractionRes = evaluator.isAppBlocked("com.unauthorized.game")
        assertTrue("Unauthorized app must be blocked in whitelist mode", distractionRes.isBlocked)
        assertTrue(distractionRes.isWhitelistBlock)
    }
}
