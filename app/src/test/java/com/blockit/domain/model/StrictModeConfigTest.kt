package com.blockit.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictModeConfigTest {

    @Test
    fun defaultStrictModeConfig_usesPinPasswordChallenge() {
        val config = StrictModeConfig()
        assertEquals(ChallengeType.PIN_PASSWORD, config.challengeType)
        assertEquals("", config.unlockPin)
        assertTrue(config.requirePinToStopSession)
        assertFalse(config.isEnabled)
    }

    @Test
    fun customPin_validatesCorrectly() {
        val config = StrictModeConfig(
            isEnabled = true,
            challengeType = ChallengeType.PIN_PASSWORD,
            unlockPin = "1234",
            requirePinToStopSession = true
        )
        assertTrue(config.unlockPin == "1234")
        assertFalse(config.unlockPin == "0000")
        assertEquals(4, config.unlockPin.length)
    }
}
