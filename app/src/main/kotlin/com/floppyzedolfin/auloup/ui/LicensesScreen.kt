package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R

/** The open-source components Au loup! bundles, and their licenses. */
@Composable
internal fun LicensesScreen(onBack: () -> Unit) {
    AuLoupScaffold(
        title = { Text(stringResource(R.string.licenses)) },
        onBack = onBack,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Au loup! is licensed under the GPL-3.0. It bundles these " +
                    "open-source components, each under the Apache License 2.0:",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "• libphonenumber-android (io.michaelrocks) — © Google Inc.; " +
                    "Android port © Michael Rozumyanskiy\n" +
                    "• AndroidX / Jetpack & Jetpack Compose — © The Android Open Source Project\n" +
                    "• Kotlin standard library & kotlinx.coroutines — © JetBrains s.r.o.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0 " +
                    "— full notices are in THIRD_PARTY_LICENSES in the project source.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
