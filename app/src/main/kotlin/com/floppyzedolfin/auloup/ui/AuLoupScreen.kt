package com.floppyzedolfin.auloup.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.floppyzedolfin.auloup.data.PrefixRepository

/**
 * Where the app currently is, beyond the home screen. Modelling it as one
 * mutually-exclusive value (rather than several independent flags) means two
 * screens can never be "open" at once. Home is simply a null destination.
 */
private sealed interface Destination {
    data object Settings : Destination
    data object History : Destination

    /** The blocked-call history for a single [prefix]. */
    data class PrefixDetail(val prefix: String) : Destination
}

/** Saves the current [Destination] across configuration changes / process death. */
private val DestinationSaver = listSaver<Destination?, String>(
    save = { dest ->
        when (dest) {
            null -> emptyList()
            Destination.Settings -> listOf("settings")
            Destination.History -> listOf("history")
            is Destination.PrefixDetail -> listOf("prefix", dest.prefix)
        }
    },
    restore = { saved ->
        when (saved.firstOrNull()) {
            "settings" -> Destination.Settings
            "history" -> Destination.History
            "prefix" -> Destination.PrefixDetail(saved[1])
            else -> null
        }
    },
)

/**
 * App navigation host: runs the first-launch side effects, then flat-switches
 * between the main screen and the Settings / History / per-prefix detail screens.
 * Hosting the nav here (above the backdrop's content slot) keeps each screen
 * small and the wolf backdrop shared across all of them.
 */
@Composable
internal fun AuLoupScreen(repository: PrefixRepository) {
    val notificationsEnabled by repository.notificationsEnabled.collectAsState(initial = true)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The toggle reflects the user's intent regardless of the OS answer. */ }

    // First launch: ask for notification permission so the default-on
    // notifications can show, and seed the official prefixes. Both run once.
    LaunchedEffect(Unit) {
        if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) { repository.seedDefaultsIfNeeded() }

    var destination by rememberSaveable(stateSaver = DestinationSaver) {
        mutableStateOf<Destination?>(null)
    }
    val goHome = { destination = null }

    when (val current = destination) {
        Destination.Settings -> SettingsScreen(repository, onBack = goHome)
        Destination.History -> HistoryScreen(repository, onBack = goHome)
        is Destination.PrefixDetail ->
            BlockedCallsScreen(repository, current.prefix, onBack = goHome)
        null -> MainScreen(
            repository = repository,
            onOpenHistory = { destination = Destination.History },
            onOpenSettings = { destination = Destination.Settings },
            onSelectPrefix = { destination = Destination.PrefixDetail(it) },
        )
    }
}
