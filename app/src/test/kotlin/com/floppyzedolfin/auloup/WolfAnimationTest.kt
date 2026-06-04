package com.floppyzedolfin.auloup

import com.floppyzedolfin.auloup.ui.WolfAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WolfAnimationTest {

    @Test
    fun atEachEndOfTheEventSheIsAsleepWithZzzShowing() {
        // progress 0 = just woke from sleep, 1 = back asleep; both: eyes shut, zzz on.
        assertEquals(0f, WolfAnimation.eyesOpen(0f), 1e-4f)
        assertEquals(1f, WolfAnimation.zzzGate(0f), 1e-4f)
        assertEquals(0f, WolfAnimation.eyesOpen(1f), 1e-4f)
        assertEquals(1f, WolfAnimation.zzzGate(1f), 1e-4f)
    }

    @Test
    fun eyesHoldFullyOpenForAboutASecond() {
        for (p in listOf(0.32f, 0.45f, 0.54f)) {
            assertEquals("held open at $p", 1f, WolfAnimation.eyesOpen(p), 1e-4f)
        }
    }

    @Test
    fun twoQuickBlinksAfterTheHold() {
        assertEquals(0f, WolfAnimation.eyesOpen(0.575f), 1e-4f) // blink 1 fully closed
        assertEquals(1f, WolfAnimation.eyesOpen(0.60f), 1e-4f) // open between blinks
        assertEquals(0f, WolfAnimation.eyesOpen(0.625f), 1e-4f) // blink 2 fully closed
    }

    @Test
    fun zzzAreOffThroughTheBlink() {
        for (p in listOf(0.2f, 0.5f, 0.8f)) {
            assertEquals("zzz hidden at $p", 0f, WolfAnimation.zzzGate(p), 1e-4f)
        }
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
            assertTrue(WolfAnimation.eyesOpen(p) in 0f..1f)
            assertTrue(WolfAnimation.zzzGate(p) in 0f..1f)
            p += 0.005f
        }
    }

    @Test
    fun sleepIsLongAndVariesBetweenBlinks() {
        val random = Random(42)
        val draws = List(200) { WolfAnimation.nextSleepMillis(random) }
        // Always a long stretch...
        assertTrue("min ${draws.min()}", draws.all { it in 8_000L until 22_000L })
        // ...but not a constant one.
        assertTrue("should vary", draws.distinct().size > 1)
    }
}
