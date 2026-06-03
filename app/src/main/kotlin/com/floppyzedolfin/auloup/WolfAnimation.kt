package com.floppyzedolfin.auloup

/**
 * Pure timing for the sleepy-wolf logo, as functions of a 0..1 loop [progress].
 * Extracted from the composable so the (fiddly) blink choreography is unit-tested
 * rather than eyeballed frame by frame.
 *
 * Over one cycle she sleeps with the zzz drifting; the zzz stop, a quiet pause
 * follows, she opens her eyes for ~1s, blinks twice, closes them, and the zzz
 * resume ~1s later. (The cycle is 10s, so 0.01 ≈ 100ms.)
 */
object WolfAnimation {

    /** Eye openness, 0 (asleep) .. 1 (fully open). */
    fun eyesOpen(progress: Float): Float = when {
        progress < 0.60f -> 0f
        progress < 0.62f -> (progress - 0.60f) / 0.02f // opening
        progress < 0.72f -> 1f // held open ~1s
        progress < 0.735f -> 1f - (progress - 0.72f) / 0.015f // blink 1 down
        progress < 0.75f -> (progress - 0.735f) / 0.015f // blink 1 up
        progress < 0.765f -> 1f - (progress - 0.75f) / 0.015f // blink 2 down
        progress < 0.78f -> (progress - 0.765f) / 0.015f // blink 2 up
        progress < 0.81f -> 1f - (progress - 0.78f) / 0.03f // settling closed
        else -> 0f
    }

    /** zzz visibility, 0 (hidden) .. 1 (shown); gated off through the blink. */
    fun zzzGate(progress: Float): Float = when {
        progress < 0.48f -> 1f
        progress < 0.53f -> (0.53f - progress) / 0.05f // fading out before the pause
        progress < 0.88f -> 0f // off through the pause + blink
        else -> (progress - 0.88f) / 0.12f // resuming ~1s after the blink
    }
}
