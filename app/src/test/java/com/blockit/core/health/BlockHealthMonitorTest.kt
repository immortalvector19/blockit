package com.blockit.core.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockHealthMonitorTest {

    @Before
    fun setUp() {
        BlockHealthMonitor.reset()
    }

    @Test
    fun initialState_hasDefaultEmptyValues() {
        val snap = BlockHealthMonitor.snapshot.value
        assertFalse(snap.isAccessibilityConnected)
        assertEquals(0L, snap.lastEventTimestamp)
        assertEquals("", snap.lastPackageSeen)
        assertFalse(snap.isOverlayPermissionGranted)
        assertFalse(snap.isBatteryExempt)
        assertEquals(0, snap.loadedBlocklistCount)
        assertEquals(0, snap.totalInterceptsLogged)
    }

    @Test
    fun updateAccessibilityStatus_updatesConnectedFlag() {
        BlockHealthMonitor.updateAccessibilityStatus(true)
        assertTrue(BlockHealthMonitor.snapshot.value.isAccessibilityConnected)

        BlockHealthMonitor.updateAccessibilityStatus(false)
        assertFalse(BlockHealthMonitor.snapshot.value.isAccessibilityConnected)
    }

    @Test
    fun recordAccessibilityEvent_recordsTimestampAndPackage() {
        val testTime = 1728000000000L
        BlockHealthMonitor.recordAccessibilityEvent("com.bolt.android", testTime)

        val snap = BlockHealthMonitor.snapshot.value
        assertEquals("com.bolt.android", snap.lastPackageSeen)
        assertEquals(testTime, snap.lastEventTimestamp)
    }

    @Test
    fun updatePermissions_recordsOverlayAndBatteryState() {
        BlockHealthMonitor.updatePermissions(overlayGranted = true, batteryExempt = true)

        val snap = BlockHealthMonitor.snapshot.value
        assertTrue(snap.isOverlayPermissionGranted)
        assertTrue(snap.isBatteryExempt)
    }

    @Test
    fun updateActiveRules_recordsSessionTelemetry() {
        BlockHealthMonitor.updateActiveRules(
            sessionProfileName = "Locking: right",
            blocklistCount = 3,
            isWhitelist = false,
            interceptCount = 2
        )

        val snap = BlockHealthMonitor.snapshot.value
        assertEquals("Locking: right", snap.activeSessionProfileName)
        assertEquals(3, snap.loadedBlocklistCount)
        assertFalse(snap.isWhitelistActive)
        assertEquals(2, snap.totalInterceptsLogged)
    }
}
