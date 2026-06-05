package com.floppyzedolfin.auloup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.Country

/**
 * Compact country selector: the collapsed box shows only the flag; the dropdown
 * lists the full name and calling code (with type-to-filter search).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CountryPicker(
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
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
    )
}

/** The one logo size used everywhere, so it never changes between screens. */
private val AppLogoSize = 40.dp

/** The wolf logo: Iris on her black circle, the same on every screen and theme. */
@Composable
private fun AppLogo(modifier: Modifier = Modifier, size: Dp = AppLogoSize) {
    Image(
        painter = painterResource(R.drawable.iris_logo),
        contentDescription = null,
        // Clip the square artwork to a circle (only the image — LogoNavIcon's
        // corner back badge sits outside this and must not be clipped).
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

/**
 * The wolf logo in the top bar's navigation slot — same position on every
 * screen. On the main screen it is just the brand mark; on sub-screens [onBack]
 * is set, so it becomes the tappable "back" control and gains a bold round badge
 * with a back arrow in the corner.
 */
@Composable
internal fun LogoNavIcon(onBack: (() -> Unit)? = null) {
    Box(
        // No clip here: a CircleShape clip would cut off the corner back badge.
        modifier = Modifier
            .padding(start = 4.dp)
            .then(if (onBack != null) Modifier.clickable(onClick = onBack) else Modifier)
            .padding(2.dp),
    ) {
        AppLogo()
        if (onBack != null) {
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
}
