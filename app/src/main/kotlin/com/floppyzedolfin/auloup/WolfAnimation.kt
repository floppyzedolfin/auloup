package com.floppyzedolfin.auloup

import kotlin.random.Random

/**
 * Pure timing for the sleepy-wolf logo. The sleep phase and the blink are
 * decoupled: she sleeps (eyes closed, zzz drifting) for a random, fairly long
 * stretch, then plays one fixed-length blink "event". [eyesOpen] and [zzzGate]
 * describe that event as functions of its 0..1 progress; sleeping is just
 * "progress held at 0". Extracted from the composable so the choreography is
 * unit-tested rather than eyeballed.
 *
 * The event ([EVENT_MILLIS] long, so 0.01 ≈ 40ms): the zzz fade out, a quiet
 * pause, eyes open ~1s, two quick blinks, settle closed, zzz resume ~1s later.
 */
object WolfAnimation {

    /** Duration of one blink event, in milliseconds. */
    const val EVENT_MILLIS = 4000

    private const val SLEEP_MIN_MILLIS = 8_000L
    private const val SLEEP_MAX_MILLIS = 22_000L

    /** A random sleep stretch between blinks — long, and a different length each time. */
    fun nextSleepMillis(random: Random = Random): Long =
        random.nextLong(SLEEP_MIN_MILLIS, SLEEP_MAX_MILLIS)

    /** Eye openness over the event, 0 (asleep) .. 1 (fully open). */
    fun eyesOpen(progress: Float): Float = when {
        progress < 0.25f -> 0f
        progress < 0.30f -> (progress - 0.25f) / 0.05f // opening
        progress < 0.55f -> 1f // held open ~1s
        progress < 0.575f -> 1f - (progress - 0.55f) / 0.025f // blink 1 down
        progress < 0.60f -> (progress - 0.575f) / 0.025f // blink 1 up
        progress < 0.625f -> 1f - (progress - 0.60f) / 0.025f // blink 2 down
        progress < 0.65f -> (progress - 0.625f) / 0.025f // blink 2 up
        progress < 0.70f -> 1f - (progress - 0.65f) / 0.05f // settling closed
        else -> 0f
    }

    /** zzz visibility over the event, 0 (hidden) .. 1 (shown). 1 at both ends so
     *  it joins the sleep phase seamlessly. */
    fun zzzGate(progress: Float): Float = when {
        progress < 0.10f -> 1f - progress / 0.10f // fading out as the blink begins
        progress < 0.90f -> 0f // off through the pause + blink
        else -> (progress - 0.90f) / 0.10f // resuming ~1s after the blink
    }
}
