package com.floppyzedolfin.auloup.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.data.BlockedCall
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.Country
import com.floppyzedolfin.auloup.telephony.PhoneFormat
import java.text.DateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

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

/**
 * The shared screen frame: a transparent [Scaffold] (so the Iris backdrop shows
 * through) topped by a [TopAppBar] whose navigation slot always holds the wolf
 * logo. Pass [onBack] on a sub-screen to make the logo the back control and wire
 * the system back gesture to it; leave it null on the home screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuLoupScaffold(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    if (onBack != null) BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = { LogoNavIcon(onBack = onBack) },
                actions = actions,
            )
        },
        content = content,
    )
}

/** Transparent ListItem container, so the Iris backdrop shows through the row. */
@Composable
internal fun transparentListItemColors() = ListItemDefaults.colors(containerColor = Color.Transparent)

/** The shared date+time format ("MEDIUM date, SHORT time") for blocked-call rows. */
@Composable
internal fun rememberCallTimeFormatter(): DateFormat =
    remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

/**
 * One blocked-call row: the caller's number (formatted for [country], or
 * "unknown" when withheld) over the time it was blocked. Set [showFlag] to put
 * the country flag in the leading slot — used in the all-prefixes history where
 * rows mix countries; the per-prefix screen already shows the flag in its title.
 *
 * Tapping a row with a known number opens the dialer pre-filled with it, so a
 * blocked caller can still be rung back from the history.
 */
@Composable
internal fun BlockedCallRow(
    call: BlockedCall,
    country: Country?,
    formatter: DateFormat,
    showFlag: Boolean = false,
) {
    val context = LocalContext.current
    // Withheld/unknown calls have no number to dial; only known ones are tappable.
    val rowModifier = if (call.number.isNotBlank()) {
        Modifier.clickable { dial(context, call.number) }
    } else {
        Modifier
    }
    ListItem(
        modifier = rowModifier,
        colors = transparentListItemColors(),
        leadingContent = if (showFlag) country?.flag?.let { flag -> { Text(flag) } } else null,
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
        // An explicit call button, in addition to the tappable row, so it's
        // clear the number can be rung back.
        trailingContent = if (call.number.isNotBlank()) {
            {
                IconButton(onClick = { dial(context, call.number) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_call),
                        contentDescription = stringResource(R.string.call_back),
                    )
                }
            }
        } else {
            null
        },
    )
}

/**
 * Opens the system dialer pre-filled with [number]. Uses ACTION_DIAL (no
 * CALL_PHONE permission, and it never places the call itself — the user does),
 * so calling back stays consistent with the app's no-extra-permissions stance.
 */
private fun dial(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
    runCatching { context.startActivity(intent) }
}

/** The active locale, read observably from the configuration. */
@Composable
internal fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Localized "Month Year", e.g. "June 2026" / "Juin 2026". */
internal fun monthYearLabel(yearMonth: YearMonth, locale: Locale): String =
    yearMonth.month.getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) } + " " + yearMonth.year

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
