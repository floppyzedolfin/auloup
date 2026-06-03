package com.floppyzedolfin.auloup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WolfAnimationTest {

    @Test
    fun asleep_eyesClosedAndZzzVisible() {
        for (p in listOf(0f, 0.2f, 0.4f)) {
            assertEquals("eyes at $p", 0f, WolfAnimation.eyesOpen(p), 1e-4f)
            assertEquals("zzz at $p", 1f, WolfAnimation.zzzGate(p), 1e-4f)
        }
    }

    @Test
    fun eyesHoldFullyOpenForAboutASecond() {
        // The ~1s hold sits between 0.62 and 0.72 of the 10s cycle.
        for (p in listOf(0.63f, 0.67f, 0.71f)) {
            assertEquals("held open at $p", 1f, WolfAnimation.eyesOpen(p), 1e-4f)
        }
    }

    @Test
    fun twoQuickBlinksAfterTheHold() {
        // Eyes dip closed twice (the blinks) but reopen between them.
        assertEquals(0f, WolfAnimation.eyesOpen(0.735f), 1e-4f) // blink 1 fully closed
        assertEquals(1f, WolfAnimation.eyesOpen(0.75f), 1e-4f) // open between blinks
        assertEquals(0f, WolfAnimation.eyesOpen(0.765f), 1e-4f) // blink 2 fully closed
    }

    @Test
    fun zzzAreOffThroughTheBlinkAndBackByEndOfCycle() {
        for (p in listOf(0.6f, 0.7f, 0.85f)) {
            assertEquals("zzz hidden at $p", 0f, WolfAnimation.zzzGate(p), 1e-4f)
        }
        assertEquals("fully resumed at the cycle end", 1f, WolfAnimation.zzzGate(1f), 1e-4f)
        assertEquals("asleep again at start", 0f, WolfAnimation.eyesOpen(0.999f), 1e-4f)
    }

    @Test
    fun zzzAreNeverShownWhileTheEyesAreOpen() {
        var p = 0f
        while (p <= 1f) {
            if (WolfAnimation.eyesOpen(p) > 0f) {
                assertEquals("zzz visible while eyes open at $p", 0f, WolfAnimation.zzzGate(p), 1e-4f)
            }
            p += 0.005f
        }
    }

    @Test
    fun bothSignalsStayWithinUnitRange() {
        var p = 0f
        while (p <= 1f) {
            val e = WolfAnimation.eyesOpen(p)
            val z = WolfAnimation.zzzGate(p)
            assertTrue("eyesOpen out of range at $p: $e", e in 0f..1f)
            assertTrue("zzzGate out of range at $p: $z", z in 0f..1f)
            p += 0.005f
        }
    }
}
