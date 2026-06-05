package com.floppyzedolfin.auloup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import java.text.DateFormat
import java.util.Date

/** History of every call blocked by [prefix], most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BlockedCallsScreen(repository: PrefixRepository, prefix: String, onBack: () -> Unit) {
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
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                // No app logo here — just the flag and the formatted prefix.
                title = { Text(country?.flag?.let { "$it  $shownPrefix" } ?: shownPrefix) },
                navigationIcon = { LogoNavIcon(onBack = onBack) },
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
