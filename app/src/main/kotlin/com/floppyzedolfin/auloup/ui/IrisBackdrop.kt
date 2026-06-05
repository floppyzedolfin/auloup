package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.floppyzedolfin.auloup.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

// Blink choreography. A blink is a quick shut/open; the wake and sleep sequences
// hold the eyes a little longer (EYE_HOLD) in the middle.
private const val EYE_BLINK_MS = 120L
private const val EYE_HOLD_MS = 240L
private const val AWAKE_BLINK_INTERVAL_MS = 3_000L
private const val BLINKS_UNTIL_SLEEP = 3

/**
 * The shared backdrop: Iris resting at the bottom behind [content], plus her
 * wake/sleep cycle. She starts asleep; any touch (observed on the Initial pass,
 * without consuming) blinks her awake; while awake she blinks every few seconds
 * and, after [BLINKS_UNTIL_SLEEP] blinks with no interaction, blinks back to
 * sleep. Hosting this above the navigation keeps the state shared across screens.
 *
 * [content] is wrapped in a transparent [Surface] so the backdrop shows through
 * while bare text still defaults to onBackground (rather than black).
 */
@Composable
internal fun IrisBackdrop(
    dark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactions = remember { Channel<Unit>(Channel.CONFLATED) }
    var eyesOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        suspend fun wake() {
            eyesOpen = true
            delay(EYE_BLINK_MS)
            eyesOpen = false
            delay(EYE_HOLD_MS)
            eyesOpen = true
            delay(EYE_HOLD_MS)
            eyesOpen = false
            delay(EYE_BLINK_MS)
            eyesOpen = true
        }
        suspend fun sleep() {
            eyesOpen = false
            delay(EYE_BLINK_MS)
            eyesOpen = true
            delay(EYE_HOLD_MS)
            eyesOpen = false
            delay(EYE_HOLD_MS)
            eyesOpen = true
            delay(EYE_BLINK_MS)
            eyesOpen = false
        }
        suspend fun blink() {
            eyesOpen = false
            delay(EYE_BLINK_MS)
            eyesOpen = true
        }
        while (true) {
            interactions.receive() // asleep until the user does something
            wake()
            var blinks = 0
            while (true) {
                val acted = withTimeoutOrNull(AWAKE_BLINK_INTERVAL_MS) { interactions.receive() } != null
                if (acted) {
                    blinks = 0 // interaction keeps her awake
                } else {
                    blink()
                    if (++blinks >= BLINKS_UNTIL_SLEEP) break
                }
            }
            sleep()
        }
    }

    val irisRes = when {
        dark && eyesOpen -> R.drawable.iris_dark_cutout
        dark -> R.drawable.iris_dark_closed_cutout
        eyesOpen -> R.drawable.iris_light_cutout
        else -> R.drawable.iris_light_closed_cutout
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interactions.trySend(Unit)
                    }
                }
            },
    ) {
        Image(
            painter = painterResource(irisRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            alpha = 0.55f,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    }
}
