package com.floppyzedolfin.auloup

import android.content.Context
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil

/**
 * Display-only phone-number formatting that groups digits the way each country
 * writes them — including **incomplete** numbers, so "+155524" shows as
 * "+1 555 24" and a national "167" being typed for France shows as "1 67".
 *
 * It uses libphonenumber's [AsYouTypeFormatter][PhoneNumberUtil.getAsYouTypeFormatter],
 * which formats progressively (unlike the platform's PhoneNumberUtils.formatNumber,
 * which only groups fully valid numbers). [init] must be called once with a
 * Context before use; until then formatting is a no-op that returns the input.
 */
object PhoneFormat {

    @Volatile
    private var util: PhoneNumberUtil? = null

    /** Loads libphonenumber's metadata once (cheap to call repeatedly). */
    fun init(context: Context) {
        if (util == null) {
            synchronized(this) {
                if (util == null) {
                    util = PhoneNumberUtil.createInstance(context.applicationContext)
                }
            }
        }
    }

    /** Groups a stored prefix (a "+CC…" string, possibly partial) per [iso]. */
    fun prefix(prefix: String, iso: String?): String = group(prefix, iso)

    /**
     * Groups a blocked call's [rawNumber] (as it arrived — national or
     * international) the way [country] writes its numbers.
     */
    fun number(rawNumber: String, country: Country?): String = group(rawNumber, country?.iso)

    /**
     * Groups the national digits being typed into the input field (no country
     * code) for [iso], so spacing appears live as the user types.
     */
    fun national(input: String, iso: String?): String = group(input, iso)

    /**
     * Feeds [raw]'s '+' and digits through an as-you-type formatter so partial
     * numbers are grouped too. The [iso] region only matters for national input
     * ("+CC…" auto-detects its country). Returns [raw] unchanged if the library
     * isn't initialised or nothing formattable is present.
     */
    private fun group(raw: String, iso: String?): String {
        val phoneUtil = util ?: return raw
        val formatter = phoneUtil.getAsYouTypeFormatter(iso ?: "US")
        var formatted = ""
        for (c in raw) {
            if (c == '+' || c.isDigit()) formatted = formatter.inputDigit(c)
        }
        return formatted.ifBlank { raw }
    }
}
