package com.floppyzedolfin.auloup

import android.telephony.PhoneNumberUtils

/**
 * Display-only phone-number formatting that groups digits the way each country
 * writes them (e.g. US "(555) 320-9384", France "+33 1 67 38 92 37", UK
 * "07878 902 249").
 *
 * The grouping rules differ per country and there are hundreds of them, so we
 * don't maintain any ourselves: we defer to the platform's bundled
 * libphonenumber via [PhoneNumberUtils]. It formats in the number's original
 * style (national vs international), which is why an as-arrived national number
 * keeps its trunk "0" while an international one keeps its "+CC".
 */
object PhoneFormat {

    /**
     * Formats a stored prefix (a canonical "+CC…" string) using [iso]'s
     * grouping. Prefixes are partial numbers, so this is best-effort and falls
     * back to the raw prefix when the platform can't group it.
     */
    fun prefix(prefix: String, iso: String?): String = format(prefix, prefix, iso)

    /**
     * Formats a blocked call's [rawNumber] (as it arrived — national or
     * international) the way [country] writes its numbers. Falls back to the
     * raw number when [country] is unknown or it can't be formatted.
     */
    fun number(rawNumber: String, country: Country?): String {
        if (country == null) return rawNumber
        val e164 = Prefixes.toInternational(rawNumber, country.dialCode, country.trunkPrefix) ?: rawNumber
        return format(rawNumber, e164, country.iso)
    }

    /**
     * [display] is the number to format (its national/international style is
     * preserved), [e164] its international form (used to pick the calling code),
     * and [iso] the fallback region. Returns [display] unchanged when it can't
     * be formatted.
     */
    private fun format(display: String, e164: String, iso: String?): String {
        if (iso.isNullOrBlank()) return display
        return PhoneNumberUtils.formatNumber(display, e164, iso)?.takeIf { it.isNotBlank() } ?: display
    }
}
