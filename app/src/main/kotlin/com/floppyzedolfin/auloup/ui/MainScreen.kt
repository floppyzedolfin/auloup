package com.floppyzedolfin.auloup.ui

import android.app.role.RoleManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixData
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import com.floppyzedolfin.auloup.telephony.Prefixes
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * The home screen: today's blocked count + history/settings actions in the bar,
 * the "blocking is off" hint, the add-a-prefix row, and the prefix list grouped
 * by country (each row toggle-able; user prefixes deletable).
 */
@Composable
internal fun MainScreen(
    repository: PrefixRepository,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectPrefix: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefixes by repository.prefixes.collectAsState(initial = emptyList())
    val allCalls by repository.allCalls.collectAsState(initial = emptyList())

    // Whether Au loup! holds the call-screening role, for the "blocking is off"
    // hint. Read each composition so it refreshes when returning from Settings.
    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    val blockingEnabled = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

    // Today's blocked-call count, shown next to the logo.
    val zone = remember { ZoneId.systemDefault() }
    val blockedToday = remember(allCalls) {
        PrefixData.blockedOnDayOf(allCalls, System.currentTimeMillis(), zone)
    }

    var country by remember { mutableStateOf(Countries.defaultFor(context)) }
    var number by remember { mutableStateOf(TextFieldValue()) }

    // Group the prefixes by their country (derived from the calling code).
    val groupedPrefixes = remember(prefixes) {
        prefixes
            .groupBy { Countries.countryForPrefix(it.prefix) }
            .entries
            .sortedBy { it.key?.displayName ?: "" }
    }
    val collapsedCountries = remember { mutableStateListOf<String>() }

    AuLoupScaffold(
        // Home screen: the logo is just the brand mark (no back action).
        title = {
            Text(
                pluralStringResource(R.plurals.calls_blocked_today, blockedToday, blockedToday),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        actions = {
            IconButton(onClick = onOpenHistory) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = stringResource(R.string.history_title),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_title),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Hint when another app (or none) holds call screening, so the user
            // understands why nothing is being blocked. Tap to fix it in Settings.
            if (!blockingEnabled) {
                ElevatedCard(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(R.string.blocking_off_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            stringResource(R.string.blocking_off_message),
                            style = MaterialTheme.typography.bodySmall,
                        )
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
                val prefix = remember(country, number.text) {
                    Prefixes.buildPrefix(country.dialCode, country.trunkPrefix, number.text)
                }
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
                                // Dim a disabled prefix so its inactive state reads at a glance.
                                val dim = if (entry.enabled) 1f else 0.4f
                                ListItem(
                                    // Transparent container so the Iris backdrop shows through the list.
                                    colors = transparentListItemColors(),
                                    modifier = Modifier.clickable { onSelectPrefix(entry.prefix) },
                                    headlineContent = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                PhoneFormat.prefix(entry.prefix, country?.iso),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = dim),
                                            )
                                            // Trash sits just after the number (left side).
                                            // Official prefixes are disable-only: no trashcan.
                                            if (!entry.official) {
                                                IconButton(
                                                    onClick = {
                                                        scope.launch { repository.remove(entry.prefix) }
                                                    },
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_delete),
                                                        contentDescription = stringResource(R.string.remove),
                                                        modifier = Modifier.size(20.dp),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    supportingContent = {
                                        Text(
                                            pluralStringResource(
                                                R.plurals.calls_blocked,
                                                entry.blockedCount,
                                                entry.blockedCount,
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dim),
                                        )
                                    },
                                    trailingContent = {
                                        Switch(
                                            checked = entry.enabled,
                                            onCheckedChange = {
                                                scope.launch { repository.setEnabled(entry.prefix, it) }
                                            },
                                        )
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
