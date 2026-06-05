package com.floppyzedolfin.auloup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
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
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.OfficialLists
import com.floppyzedolfin.auloup.data.PrefixRepository
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.PhoneFormat

/** Official regulator block lists the user can import (currently France). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OfficialListsScreen(
    repository: PrefixRepository,
    onImport: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val prefixes by repository.prefixes.collectAsState(initial = emptyList())
    val current = remember(prefixes) { prefixes.map { it.prefix }.toSet() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.official_lists)) },
                navigationIcon = { LogoNavIcon(onBack = onBack) },
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
