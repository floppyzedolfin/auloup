package com.floppyzedolfin.auloup.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.data.ThemeMode
import kotlinx.coroutines.launch
import java.util.Locale

/** Settings: change the app language and toggle the blocked-call notification. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(repository: PrefixRepository, onBack: () -> Unit) {
    var showLicenses by rememberSaveable { mutableStateOf(false) }
    if (showLicenses) {
        LicensesScreen(onBack = { showLicenses = false })
        return
    }
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationsEnabled by repository.notificationsEnabled.collectAsState(initial = true)
    val themeMode by repository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    var themeExpanded by remember { mutableStateOf(false) }
    val currentLanguage = remember {
        Locale.getDefault().getDisplayLanguage(Locale.getDefault())
            .replaceFirstChar { it.uppercase() }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { LogoNavIcon(onBack = onBack) },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // 1. Notification
            ListItem(
                headlineContent = { Text(stringResource(R.string.notify_label)) },
                trailingContent = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { repository.setNotificationsEnabled(enabled) }
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                },
            )
            // 2. Language
            ListItem(
                modifier = Modifier.clickable { openLanguageSettings(context) },
                leadingContent = {
                    Icon(painter = painterResource(R.drawable.ic_language), contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.language)) },
                supportingContent = { Text(currentLanguage) },
            )
            // 3. Theme (compact dropdown)
            val themeLabel = stringResource(
                when (themeMode) {
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                    else -> R.string.theme_system
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme)) },
                trailingContent = {
                    Box {
                        TextButton(onClick = { themeExpanded = true }) {
                            Text(themeLabel)
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_drop_down),
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = themeExpanded,
                            onDismissRequest = { themeExpanded = false },
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to R.string.theme_system,
                                ThemeMode.LIGHT to R.string.theme_light,
                                ThemeMode.DARK to R.string.theme_dark,
                            ).forEach { (mode, labelRes) ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(labelRes)) },
                                    onClick = {
                                        scope.launch { repository.setThemeMode(mode) }
                                        themeExpanded = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
            // 4. Open-source licenses
            ListItem(
                modifier = Modifier.clickable { showLicenses = true },
                headlineContent = { Text(stringResource(R.string.licenses)) },
            )
        }
    }
}

/** Opens the system per-app language picker (Android 13+), else app details. */
private fun openLanguageSettings(context: Context) {
    val data = Uri.fromParts("package", context.packageName, null)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, data)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, data)
    }
    context.startActivity(intent)
}
