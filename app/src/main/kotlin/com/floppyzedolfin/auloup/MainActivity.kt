package com.floppyzedolfin.auloup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.data.ThemeMode
import com.floppyzedolfin.auloup.service.Notifications
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import com.floppyzedolfin.auloup.ui.AuLoupScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannel(this)
        PhoneFormat.init(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember { PrefixRepository(context.applicationContext) }
            val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val dark = ThemeMode.isDark(themeMode, isSystemInDarkTheme())
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                // Sleepy-Iris backdrop state, shared across every screen (it lives
                // above the navigation). She starts asleep; an interaction blinks
                // her awake; she blinks every 2s while awake and, after 6s with no
                // interaction, blinks back to sleep.
                val interactions = remember { Channel<Unit>(Channel.CONFLATED) }
                var eyesOpen by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    // Wake: open, closed, longer open, closed, then settle open.
                    suspend fun wake() {
                        eyesOpen = true
                        delay(120)
                        eyesOpen = false
                        delay(120)
                        eyesOpen = true
                        delay(240)
                        eyesOpen = false
                        delay(120)
                        eyesOpen = true
                    }

                    // Sleep: the mirror — closed, open, longer closed, open, settle closed.
                    suspend fun sleep() {
                        eyesOpen = false
                        delay(120)
                        eyesOpen = true
                        delay(120)
                        eyesOpen = false
                        delay(240)
                        eyesOpen = true
                        delay(120)
                        eyesOpen = false
                    }

                    // A quick blink while awake (eyes shut briefly, then back open).
                    suspend fun blink() {
                        eyesOpen = false
                        delay(120)
                        eyesOpen = true
                    }
                    while (true) {
                        interactions.receive() // asleep until the user does something
                        wake()
                        // Awake: blink every 2s; after 6s with no interaction, sleep.
                        var idleMillis = 0L
                        while (true) {
                            val acted = withTimeoutOrNull(2_000) { interactions.receive() } != null
                            if (acted) {
                                idleMillis = 0L // interaction keeps her awake
                            } else {
                                idleMillis += 2_000L
                                if (idleMillis >= 6_000L) break
                                blink()
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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        // Observe every touch (Initial pass, without consuming) to drive
                        // the wake/sleep cycle, no matter which screen is showing.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Initial)
                                    interactions.trySend(Unit)
                                }
                            }
                        },
                ) {
                    // Iris (wireframe, background removed) resting at the bottom of
                    // the screen behind every page. Theme-aware, eyes open/closed.
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
                    // Transparent surface (so the backdrop shows) that still sets
                    // the content color, so bare Text defaults to onBackground
                    // instead of black.
                    Surface(
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        AuLoupScreen(repository)
                    }
                }
            }
        }
    }
}
