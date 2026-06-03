package com.floppyzedolfin.auloup

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale

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
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuLoupScreen(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuLoupScreen(repository: PrefixRepository) {
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
    var showOfficialLists by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(repository = repository, onBack = { showSettings = false })
        return
    }
    if (showHistory) {
        HistoryScreen(repository = repository, onBack = { showHistory = false })
        return
    }
    if (showOfficialLists) {
        OfficialListsScreen(
            repository = repository,
            // Launch on the (surviving) main-screen scope so navigating back
            // doesn't cancel the import mid-flight.
            onImport = { scope.launch { repository.importOfficial(it) } },
            onBack = { showOfficialLists = false },
        )
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
        topBar = {
            TopAppBar(
                title = { AppBarTitle(stringResource(R.string.app_name)) },
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
                .padding(16.dp),
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

            // Only offered for countries that actually have an official list.
            if (OfficialLists.forIso(country.iso) != null) {
                TextButton(onClick = { showOfficialLists = true }) {
                    Text(stringResource(R.string.official_lists))
                }
            }

            HorizontalDivider()

            StatsSection(allCalls)

            if (prefixes.isEmpty()) {
                Text(
                    stringResource(R.string.no_prefixes),
                    style = MaterialTheme.typography.bodyMedium,
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
                                    .padding(vertical = 10.dp),
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
                                ListItem(
                                    modifier = Modifier.clickable { selectedPrefix = entry.prefix },
                                    overlineContent = if (entry.official) {
                                        {
                                            Text(
                                                stringResource(R.string.official),
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    headlineContent = { Text(PhoneFormat.prefix(entry.prefix, country?.iso)) },
                                    supportingContent = {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.calls_blocked,
                                                entry.blockedCount,
                                                entry.blockedCount,
                                            ),
                                        )
                                    },
                                    trailingContent = {
                                        TextButton(onClick = {
                                            scope.launch { repository.remove(entry.prefix) }
                                        }) {
                                            Text(stringResource(R.string.remove))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** History of every call blocked by [prefix], most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedCallsScreen(repository: PrefixRepository, prefix: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val calls by repository.history(prefix).collectAsState(initial = emptyList())
    val formatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    // The country this prefix belongs to: its flag and grouping go in the title,
    // and it formats each blocked call's number below.
    val country = remember(prefix) { Countries.countryForPrefix(prefix) }
    val shownPrefix = remember(prefix, country) { PhoneFormat.prefix(prefix, country?.iso) }

    Scaffold(
        topBar = {
            TopAppBar(
                // No app logo here — just the flag and the formatted prefix.
                title = { Text(country?.flag?.let { "$it  $shownPrefix" } ?: shownPrefix) },
                navigationIcon = { LogoBackButton(onBack) },
            )
        },
    ) { innerPadding ->
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.no_calls_for_prefix),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                item {
                    StatsSection(
                        calls = calls,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    )
                    HorizontalDivider()
                }
                items(calls) { call ->
                    ListItem(
                        headlineContent = {
                            Text(
                                if (call.number.isBlank()) {
                                    stringResource(R.string.unknown_number)
                                } else {
                                    PhoneFormat.number(call.number, country)
                                },
                            )
                        },
                        supportingContent = { Text(formatter.format(Date(call.timeMillis))) },
                    )
                }
            }
        }
    }
}

/** The full history of blocked calls across every prefix, most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(repository: PrefixRepository, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val calls by repository.allCalls.collectAsState(initial = emptyList())
    val formatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = { LogoBackButton(onBack) },
            )
        },
    ) { innerPadding ->
        if (calls.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.no_calls_yet),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(calls) { call ->
                    val country = Countries.countryForPrefix(call.prefix)
                    ListItem(
                        leadingContent = country?.flag?.let { flag -> { Text(flag) } },
                        headlineContent = {
                            Text(
                                if (call.number.isBlank()) {
                                    stringResource(R.string.unknown_number)
                                } else {
                                    PhoneFormat.number(call.number, country)
                                },
                            )
                        },
                        supportingContent = { Text(formatter.format(Date(call.timeMillis))) },
                    )
                }
            }
        }
    }
}

/**
 * Compact country selector: the collapsed box shows only the flag; the dropdown
 * lists the full name and calling code (with type-to-filter search).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryPicker(
    selected: Country,
    onSelected: (Country) -> Unit,
    modifier: Modifier = Modifier,
) {
    val countries = remember { Countries.all() }
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) {
            countries
        } else {
            val digits = query.filter { it.isDigit() }
            countries.filter { country ->
                country.displayName.contains(query, ignoreCase = true) ||
                    (digits.isNotEmpty() && country.dialCode.toString().contains(digits))
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = if (expanded) query else selected.flag,
            onValueChange = {
                query = it
                expanded = true
            },
            readOnly = !expanded,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                query = ""
            },
            matchTextFieldWidth = false,
        ) {
            filtered.forEach { country ->
                DropdownMenuItem(
                    text = { Text("${country.flag}  ${country.displayName}  +${country.dialCode}") },
                    onClick = {
                        onSelected(country)
                        query = ""
                        expanded = false
                    },
                )
            }
        }
    }
}

/** A small caption shown above an input field (its title, on the top). */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

/** A top-bar title that shows the app logo next to [text]. */
@Composable
private fun AppBarTitle(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppLogo(size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

/**
 * The sleepy-wolf logo, with three "z"s drifting up from its muzzle and fading,
 * staggered so there's always one in the air. Purely decorative.
 */
@Composable
private fun AppLogo(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val transition = rememberInfiniteTransition(label = "wolf")
    Box(modifier = modifier.size(size)) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        repeat(3) { i ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    initialStartOffset = StartOffset(i * 1000),
                ),
                label = "z$i",
            )
            val fade = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.85f -> (1f - progress) / 0.15f
                else -> 1f
            }
            // The "z" shrinks a touch as it floats up.
            val zFont = size * (0.34f - 0.06f * progress)
            // Glyph CENTRE travels diagonally from the muzzle (the middle of the
            // circle) out to the top-right corner, fading only once it gets there.
            // We anchor the Text by its top-left, so convert the wanted centre to
            // that corner — a glyph sits ~0.62em below the box top (so the trail
            // starts at the muzzle, not the chin) and ~0.3em to each side.
            val centreX = size * (0.46f + 0.4f * progress)
            val centreY = size * (0.52f - 0.46f * progress)
            Text(
                text = "z",
                color = Color(0xFFECEFF1),
                fontWeight = FontWeight.Bold,
                fontSize = with(LocalDensity.current) { zFont.toSp() },
                // No font padding, so the glyph sits at a predictable place in its
                // box and the trail lands the same way at any logo size.
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = centreX - zFont * 0.3f, y = centreY - zFont * 0.5f)
                    .alpha(fade),
            )
        }
    }
}

/**
 * The wolf logo used as the "back" affordance on sub-screens: tapping it returns
 * to the main page. A bold round badge with a back arrow sits in the corner so
 * it clearly — and cheerfully — reads as "go back".
 */
@Composable
private fun LogoBackButton(onBack: () -> Unit) {
    Box(
        // No clip here: a CircleShape clip would cut off the corner back badge.
        modifier = Modifier
            .padding(start = 4.dp)
            .clickable(onClick = onBack)
            .padding(2.dp),
    ) {
        AppLogo(size = 44.dp)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Settings: change the app language and toggle the blocked-call notification. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(repository: PrefixRepository, onBack: () -> Unit) {
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
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { LogoBackButton(onBack) },
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

/** Official regulator block lists the user can import (currently France). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficialListsScreen(
    repository: PrefixRepository,
    onImport: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val prefixes by repository.prefixes.collectAsState(initial = emptyList())
    val current = remember(prefixes) { prefixes.map { it.prefix }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.official_lists)) },
                navigationIcon = { LogoBackButton(onBack) },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(OfficialLists.all, key = { it.iso }) { list ->
                val country = Countries.forIso(list.iso)
                val imported = list.prefixes.all { it in current }
                ListItem(
                    headlineContent = {
                        Text(country?.let { "${it.flag}  ${it.displayName}" } ?: list.iso)
                    },
                    supportingContent = {
                        Text(list.prefixes.joinToString(", ") { PhoneFormat.prefix(it, list.iso) })
                    },
                    trailingContent = {
                        Button(
                            onClick = {
                                onImport(list.prefixes)
                                onBack()
                            },
                            enabled = !imported,
                        ) {
                            Text(stringResource(R.string.import_action))
                        }
                    },
                )
            }
        }
    }
}

/**
 * Two swipeable bar charts of the given blocked [calls]: one per day, one by
 * hour of day. Renders nothing when there are no calls.
 */
@Composable
private fun StatsSection(calls: List<BlockedCall>, modifier: Modifier = Modifier) {
    if (calls.isEmpty()) return
    val zone = remember { ZoneId.systemDefault() }
    val now = remember(calls) { System.currentTimeMillis() }
    val times = remember(calls) { calls.map { it.timeMillis } }
    val dayStat = remember(times) { Stats.perDay(times, days = 7, zone = zone, nowMillis = now) }
    val hourCounts = remember(times) { Stats.perHour(times, zone) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Total blocked "since the beginning".
        Text(
            pluralStringResource(R.plurals.calls_blocked, calls.size, calls.size),
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalPager(state = pagerState) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (page == 0) R.string.stats_per_day else R.string.stats_by_hour),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (page == 0) {
                    BarChart(dayStat.counts)
                    AxisLabels(dayStat.labels)
                } else {
                    BarChart(hourCounts)
                    // Hour axis 0..24, with 24 pinned to the far right.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("0", "6", "12", "18", "24").forEach {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            repeat(2) { index ->
                val color = if (pagerState.currentPage == index) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
                Box(Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}

/** Evenly-spaced labels under a [BarChart], one per bar. */
@Composable
private fun AxisLabels(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * A minimal bar chart: one bar per value, with the value printed above each
 * non-zero bar. Bars are bottom-aligned and scaled to the largest value.
 */
@Composable
private fun BarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val maxValue = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEach { value ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (value > 0) value.toString() else "",
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(value.toFloat() / maxValue)
                            .background(barColor, RoundedCornerShape(2.dp)),
                    )
                }
            }
        }
    }
}
