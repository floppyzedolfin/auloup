package com.floppyzedolfin.callbloker

/**
 * Pure phone-number-prefix logic, deliberately free of any Android dependency
 * so it can be unit-tested on the JVM. Both the UI and the call-screening
 * service go through here.
 */
object Prefixes {

    /** A blocked prefix must have at least this many national (significant) digits. */
    const val MIN_NATIONAL_DIGITS = 3

    /**
     * Canonicalises a prefix or number to a leading optional '+' followed by
     * digits only, dropping spaces, dashes and parentheses. Returns null when
     * no digit remains.
     */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        val plus = if (trimmed.startsWith("+")) "+" else ""
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.isEmpty()) null else plus + digits
    }

    /**
     * The national significant digits of [rawNational]: digits only, with a
     * leading trunk prefix (e.g. the French "0" in "01 60") removed. So both
     * "01 60" and "1 60" yield "160".
     */
    fun nationalDigits(rawNational: String, trunkPrefix: String): String {
        val digits = rawNational.filter { it.isDigit() }
        return if (trunkPrefix.isNotEmpty() && digits.startsWith(trunkPrefix)) {
            digits.removePrefix(trunkPrefix)
        } else {
            digits
        }
    }

    /**
     * Builds the canonical international prefix to store from a chosen country
     * and a national prefix the user typed (with or without the trunk prefix).
     * Returns null when fewer than [MIN_NATIONAL_DIGITS] national digits remain.
     */
    fun buildPrefix(dialCode: Int, trunkPrefix: String, rawNational: String): String? {
        val national = nationalDigits(rawNational, trunkPrefix)
        return if (national.length < MIN_NATIONAL_DIGITS) null else "+$dialCode$national"
    }

    /**
     * Canonicalises an incoming call's number to international form (+CC…) so it
     * can be matched against stored prefixes:
     * - already-international ("+…") is kept;
     * - an international-access "00…" becomes "+…";
     * - a national number ("0160…") is interpreted in the device's [localDialCode]
     *   / [localTrunk] region: trunk dropped, country code prepended.
     * Returns null when there are no digits.
     */
    fun toInternational(raw: String, localDialCode: Int, localTrunk: String): String? {
        val trimmed = raw.trim()
        if (trimmed.startsWith("+")) return normalize(trimmed)
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        if (digits.startsWith("00")) return "+" + digits.removePrefix("00")
        val national = if (localTrunk.isNotEmpty() && digits.startsWith(localTrunk)) {
            digits.removePrefix(localTrunk)
        } else {
            digits
        }
        return "+$localDialCode$national"
    }

    /**
     * The single most specific (longest) stored prefix that [number] starts
     * with, or null if none match. When several prefixes match the same call,
     * the longest one wins — e.g. with "+3316212" and "+331621" stored, the
     * call "+33162123455" matches "+3316212". Returns the prefix as stored.
     */
    fun longestMatch(number: String, prefixes: Collection<String>): String? {
        val n = normalize(number) ?: return null
        return prefixes
            .mapNotNull { stored -> normalize(stored)?.let { stored to it } }
            .filter { (_, normalized) -> n.startsWith(normalized) }
            .maxByOrNull { (_, normalized) -> normalized.length }
            ?.first
    }

    /**
     * True when [number] starts with any of the [prefixes]. Both sides are
     * normalized first, so "+1 (555)…" and "+1555…" compare equal.
     */
    fun isBlocked(number: String, prefixes: Collection<String>): Boolean = longestMatch(number, prefixes) != null
}
