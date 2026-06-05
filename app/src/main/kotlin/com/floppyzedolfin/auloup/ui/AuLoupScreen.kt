package com.floppyzedolfin.auloup.ui

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import com.floppyzedolfin.auloup.telephony.Prefixes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuLoupScreen(repository: PrefixRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefixes by repository.prefixes.collectAsState(initial = emptyList())
    val allCalls by repository.allCalls.collectAsState(initial = emptyList())
    val notificationsEnabled by repository.notificationsEnabled.collectAsState(initial = true)

    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    var roleHeld by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The toggle reflects the user's intent regardless of the OS answer. */ }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Ask once on first launch so the default-on notifications can actually show.
    LaunchedEffect(Unit) { if (notificationsEnabled) requestNotificationPermission() }

    var country by remember { mutableStateOf(Countries.defaultFor(context)) }
    var number by remember { mutableStateOf(TextFieldValue()) }
    var selectedPrefix by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    // Seed the device region's official prefixes on first launch (runs once).
    LaunchedEffect(Unit) { repository.seedDefaultsIfNeeded() }

    if (showSettings) {
        SettingsScreen(repository = repository, onBack = { showSettings = false })
        return
    }
    if (showHistory) {
        HistoryScreen(repository = repository, onBack = { showHistory = false })
        return
    }
    val viewing = selectedPrefix
    if (viewing != null) {
        BlockedCallsScreen(
            repository = repository,
            prefix = viewing,
            onBack = { selectedPrefix = null },
        )
        return
    }

    // Group the prefixes by their country (derived from the calling code).
    val groupedPrefixes = remember(prefixes) {
        prefixes
            .groupBy { Countries.countryForPrefix(it.prefix) }
            .entries
            .sortedBy { it.key?.displayName ?: "" }
    }
    val collapsedCountries = remember { mutableStateListOf<String>() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                // Logo lives in the navigation slot on every screen so it never
                // shifts; here it's just the brand mark (no back action).
                navigationIcon = { LogoNavIcon() },
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { showHistory = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_history),
                            contentDescription = stringResource(R.string.history_title),
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header is padded; the prefix list below goes full-width so a row's
            // trashcan can sit flush to the screen edge.
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (roleHeld) {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.blocking_on)) })
                } else {
                    ElevatedCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                stringResource(R.string.blocking_off_title),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(R.string.blocking_off_message))
                            Button(onClick = {
                                roleLauncher.launch(
                                    roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                                )
                            }) {
                                Text(stringResource(R.string.enable_blocking))
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(modifier = Modifier.width(96.dp)) {
                        FieldLabel(stringResource(R.string.country_label))
                        CountryPicker(
                            selected = country,
                            onSelected = { country = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FieldLabel(stringResource(R.string.number_prefix_label))
                        OutlinedTextField(
                            value = number,
                            // Group the national digits as the user types (the digits
                            // are stripped again when the prefix is built). Keep the
                            // caret at the end so inserting separators never reorders
                            // what the user types.
                            onValueChange = {
                                val grouped = PhoneFormat.national(it.text, country)
                                number = it.copy(text = grouped, selection = TextRange(grouped.length))
                            },
                            prefix = { Text("+${country.dialCode}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val prefix = Prefixes.buildPrefix(country.dialCode, country.trunkPrefix, number.text)
                    Button(
                        onClick = {
                            number = TextFieldValue()
                            prefix?.let { scope.launch { repository.add(it) } }
                        },
                        enabled = prefix != null,
                    ) {
                        Text(stringResource(R.string.add))
                    }
                }

                HorizontalDivider()

                StatsSection(allCalls)
            }

            if (prefixes.isEmpty()) {
                Text(
                    stringResource(R.string.no_prefixes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyColumn {
                    groupedPrefixes.forEach { (country, entries) ->
                        val key = country?.iso ?: "?"
                        val collapsed = key in collapsedCountries
                        val countryTotal = entries.sumOf { it.blockedCount }
                        item(key = "header-$key") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (collapsed) collapsedCountries.remove(key) else collapsedCountries.add(key)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = (if (collapsed) "▸  " else "▾  ") +
                                        (country?.let { "${it.flag}  ${it.displayName}" } ?: "?"),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.filters_count,
                                            entries.size,
                                            entries.size,
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.calls_blocked,
                                            countryTotal,
                                            countryTotal,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (!collapsed) {
                            items(entries, key = { it.prefix }) { entry ->
                                // Dim a disabled prefix so its inactive state reads at a glance.
                                val dim = if (entry.enabled) 1f else 0.4f
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedPrefix = entry.prefix }
                                        .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            PhoneFormat.prefix(entry.prefix, country?.iso),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                                        )
                                        Text(
                                            pluralStringResource(
                                                R.plurals.calls_blocked,
                                                entry.blockedCount,
                                                entry.blockedCount,
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                                        )
                                    }
                                    Switch(
                                        checked = entry.enabled,
                                        onCheckedChange = {
                                            scope.launch { repository.setEnabled(entry.prefix, it) }
                                        },
                                    )
                                    // Fixed-width slot keeps every Switch aligned in one column.
                                    // Official prefixes are disable-only (no trashcan); for the rest
                                    // the trashcan sits flush to the screen edge.
                                    Box(
                                        modifier = Modifier.width(40.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        if (!entry.official) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_delete),
                                                contentDescription = stringResource(R.string.remove),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .clickable {
                                                        scope.launch { repository.remove(entry.prefix) }
                                                    }
                                                    // Nudge the glyph to the edge; its transparent
                                                    // right margin is harmlessly clipped at the screen.
                                                    .offset(x = 3.dp)
                                                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
                                                    .size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
