package com.floppyzedolfin.auloup.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.floppyzedolfin.auloup.R
import com.floppyzedolfin.auloup.telephony.Countries
import com.floppyzedolfin.auloup.telephony.Country
import kotlinx.coroutines.delay

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

/**
 * The sleepy-wolf logo. Three "z"s drift from her muzzle to the top-right and
 * fade. Every so often she peeks — the zzz pause, she opens her eyes, then
 * closes them again and the zzz resume. Purely decorative.
 */
@Composable
private fun AppLogo(modifier: Modifier = Modifier, size: Dp = AppLogoSize) {
    val transition = rememberInfiniteTransition(label = "wolf")
    // Sleep (a long, random stretch) then one fixed-length blink event, forever.
    val event = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(WolfAnimation.nextSleepMillis())
            event.snapTo(0f)
            event.animateTo(1f, tween(durationMillis = WolfAnimation.EVENT_MILLIS, easing = LinearEasing))
        }
    }
    val eyesOpen = WolfAnimation.eyesOpen(event.value)
    val zzzGate = WolfAnimation.zzzGate(event.value)
    Box(modifier = modifier.size(size)) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ic_logo_eyes_open),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(eyesOpen),
        )
        repeat(3) { i ->
            val progress by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                    initialStartOffset = StartOffset(i * 1000),
                ),
                label = "z$i",
            )
            val fade = when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.85f -> (1f - progress) / 0.15f
                else -> 1f
            }
            // The "z" shrinks a touch as it floats up.
            val zFont = size * (0.34f - 0.08f * progress)
            // Glyph CENTRE travels diagonally from the muzzle (the middle of the
            // circle) out past the top-right corner — a long trail so the three
            // z's are well spaced. Anchored by top-left, so convert the wanted
            // centre to that corner (glyph sits ~0.5em below box top, ~0.3em aside).
            val centreX = size * (0.53f + 0.42f * progress)
            val centreY = size * (0.58f - 0.56f * progress)
            Text(
                text = "z",
                color = Color(0xFFECEFF1),
                fontWeight = FontWeight.Bold,
                fontSize = with(LocalDensity.current) { zFont.toSp() },
                // No font padding, so the glyph sits at a predictable place in its
                // box and the trail lands the same way at any logo size.
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = centreX - zFont * 0.3f, y = centreY - zFont * 0.5f)
                    .alpha(fade * zzzGate),
            )
        }
    }
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
