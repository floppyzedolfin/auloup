package com.floppyzedolfin.auloup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** The full history of blocked calls across every prefix, most recent first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryScreen(repository: PrefixRepository, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val calls by repository.allCalls.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }
    val formatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = { LogoNavIcon(onBack = onBack) },
                actions = {
                    if (calls.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.clear_history),
                            )
                        }
                    }
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
                    stringResource(R.string.no_calls_yet),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(calls) { call ->
                    val country = Countries.countryForPrefix(call.prefix)
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
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

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.clear_history)) },
                text = { Text(stringResource(R.string.clear_history_confirm)) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch { repository.clearHistory() }
                        showClearDialog = false
                    }) {
                        Text(
                            stringResource(R.string.remove),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
