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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.floppyzedolfin.auloup.data.PrefixRepository

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

    var selectedPrefix by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    val viewingPrefix = selectedPrefix
    when {
        showSettings -> SettingsScreen(repository, onBack = { showSettings = false })
        showHistory -> HistoryScreen(repository, onBack = { showHistory = false })
        viewingPrefix != null ->
            BlockedCallsScreen(repository, viewingPrefix, onBack = { selectedPrefix = null })
        else -> MainScreen(
            repository = repository,
            onOpenHistory = { showHistory = true },
            onOpenSettings = { showSettings = true },
            onSelectPrefix = { selectedPrefix = it },
        )
    }
}
