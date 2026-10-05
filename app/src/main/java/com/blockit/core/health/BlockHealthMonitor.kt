package com.blockit.core.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HealthSnapshot(
    val isAccessibilityConnected: Boolean = false,
    val lastEventTimestamp: Long = 0L,
    val lastPackageSeen: String = "",
    val isOverlayPermissionGranted: Boolean = false,
    val isBatteryExempt: Boolean = false,
    val activeSessionProfileName: String? = null,
    val loadedBlocklistCount: Int = 0,
    val isWhitelistActive: Boolean = false,
    val totalInterceptsLogged: Int = 0
)

object BlockHealthMonitor {
    private val _snapshot = MutableStateFlow(HealthSnapshot())
    val snapshot: StateFlow<HealthSnapshot> = _snapshot.asStateFlow()

    @Volatile
    var testBlockTrigger: (() -> Unit)? = null

    fun updateAccessibilityStatus(connected: Boolean) {
        _snapshot.value = _snapshot.value.copy(isAccessibilityConnected = connected)
    }

    fun recordAccessibilityEvent(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        _snapshot.value = _snapshot.value.copy(
            lastEventTimestamp = timestamp,
            lastPackageSeen = packageName
        )
    }

    fun updatePermissions(overlayGranted: Boolean, batteryExempt: Boolean) {
        _snapshot.value = _snapshot.value.copy(
            isOverlayPermissionGranted = overlayGranted,
            isBatteryExempt = batteryExempt
        )
    }

    fun updateActiveRules(
        sessionProfileName: String?,
        blocklistCount: Int,
        isWhitelist: Boolean,
        interceptCount: Int
    ) {
        _snapshot.value = _snapshot.value.copy(
            activeSessionProfileName = sessionProfileName,
            loadedBlocklistCount = blocklistCount,
            isWhitelistActive = isWhitelist,
            totalInterceptsLogged = interceptCount
        )
    }

    fun reset() {
        _snapshot.value = HealthSnapshot()
        testBlockTrigger = null
    }
}
