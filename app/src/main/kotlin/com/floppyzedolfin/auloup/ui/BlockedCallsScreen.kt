package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat

/** History of every call blocked by [prefix], most recent first. */
@Composable
internal fun BlockedCallsScreen(repository: PrefixRepository, prefix: String, onBack: () -> Unit) {
    val calls by repository.history(prefix).collectAsState(initial = emptyList())
    val formatter = rememberCallTimeFormatter()
    // The country this prefix belongs to: its flag and grouping go in the title,
    // and it formats each blocked call's number below.
    val country = remember(prefix) { Countries.countryForPrefix(prefix) }
    val shownPrefix = remember(prefix, country) { PhoneFormat.prefix(prefix, country?.iso) }

    AuLoupScaffold(
        // The title is just the flag and the formatted prefix.
        title = { Text(country?.flag?.let { "$it  $shownPrefix" } ?: shownPrefix) },
        onBack = onBack,
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
                    BlockedCallRow(call = call, country = country, formatter = formatter)
                }
            }
        }
    }
}
