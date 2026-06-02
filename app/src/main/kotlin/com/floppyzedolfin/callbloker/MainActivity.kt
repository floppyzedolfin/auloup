package com.floppyzedolfin.callbloker

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val roleManager = remember { context.getSystemService(RoleManager::class.java) }
    var roleHeld by remember {
        mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
    }
    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        roleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    var country by remember { mutableStateOf(Countries.defaultFor(context)) }
    var number by remember { mutableStateOf("") }

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
                AssistChip(onClick = {}, label = { Text("Call blocking is on") })
            } else {
                ElevatedCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Call blocking is off", style = MaterialTheme.typography.titleMedium)
                        Text("CallBloker must be your call-screening app to block calls.")
                        Button(onClick = {
                            roleLauncher.launch(
                                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING),
                            )
                        }) {
                            Text("Enable call blocking")
                        }
                    }
                }
            }

            CountryDropdown(selected = country, onSelected = { country = it })

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Number prefix (optional)") },
                    prefix = { Text("+${country.dialCode}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val digits = number.filter { it.isDigit() }
                        number = ""
                        scope.launch { repository.add("+${country.dialCode}$digits") }
                    },
                ) {
                    Text("Add")
                }
            }

            HorizontalDivider()

            if (prefixes.isEmpty()) {
                Text(
                    "No prefixes yet. Add one above to start blocking.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(prefixes, key = { it }) { prefix ->
                        ListItem(
                            headlineContent = { Text(prefix) },
                            trailingContent = {
                                TextButton(onClick = {
                                    scope.launch { repository.remove(prefix) }
                                }) {
                                    Text("Remove")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/** A flag + name + calling-code picker with type-to-filter search. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(selected: Country, onSelected: (Country) -> Unit) {
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
    ) {
        OutlinedTextField(
            value = if (expanded) query else "${selected.flag}  ${selected.displayName}  +${selected.dialCode}",
            onValueChange = {
                query = it
                expanded = true
            },
            readOnly = !expanded,
            singleLine = true,
            label = { Text("Country") },
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
