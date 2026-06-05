package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.PrefixRepository
import kotlinx.coroutines.launch

/** The full history of blocked calls across every prefix, most recent first, with month navigation. */
@Composable
internal fun HistoryScreen(repository: PrefixRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val calls by repository.allCalls.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    AuLoupScaffold(
        title = { Text(stringResource(R.string.history_title)) },
        onBack = onBack,
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
            // Rows mix countries here, so each shows its flag.
            MonthlyBlockedCalls(
                calls = calls,
                showFlag = true,
                modifier = Modifier.padding(innerPadding),
            )
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
