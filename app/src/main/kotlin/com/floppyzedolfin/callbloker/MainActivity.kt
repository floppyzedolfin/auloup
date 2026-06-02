package com.floppyzedolfin.callbloker

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannel(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CallBlokerScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallBlokerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PrefixRepository(context.applicationContext) }
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
    var number by remember { mutableStateOf("") }
    var selectedPrefix by rememberSaveable { mutableStateOf<String?>(null) }

    val viewing = selectedPrefix
    if (viewing != null) {
        BlockedCallsScreen(
            repository = repository,
            prefix = viewing,
            onBack = { selectedPrefix = null },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.notify_label))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { repository.setNotificationsEnabled(enabled) }
                        if (enabled) requestNotificationPermission()
                    },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountryPicker(
                    selected = country,
                    onSelected = { country = it },
                    modifier = Modifier.width(132.dp),
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.number_prefix_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                )
                val prefix = Prefixes.buildPrefix(country.dialCode, country.trunkPrefix, number)
                Button(
                    onClick = {
                        number = ""
                        prefix?.let { scope.launch { repository.add(it) } }
                    },
                    enabled = prefix != null,
                ) {
                    Text(stringResource(R.string.add))
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
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(prefixes, key = { it.prefix }) { entry ->
                        ListItem(
                            modifier = Modifier.clickable { selectedPrefix = entry.prefix },
                            headlineContent = { Text(entry.prefix) },
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

/** History of every call blocked by [prefix], most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedCallsScreen(repository: PrefixRepository, prefix: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val calls by repository.history(prefix).collectAsState(initial = emptyList())
    val formatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(prefix) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
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
                            Text(call.number.ifBlank { stringResource(R.string.unknown_number) })
                        },
                        supportingContent = { Text(formatter.format(Date(call.timeMillis))) },
                    )
                }
            }
        }
    }
}

/**
 * Compact country selector: the collapsed box shows only the flag and calling
 * code; the dropdown lists the full name (with type-to-filter search).
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
            value = if (expanded) query else "${selected.flag} +${selected.dialCode}",
            onValueChange = {
                query = it
                expanded = true
            },
            readOnly = !expanded,
            singleLine = true,
            label = { Text(stringResource(R.string.country_label)) },
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
