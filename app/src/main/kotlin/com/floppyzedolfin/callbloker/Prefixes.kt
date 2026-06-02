package com.floppyzedolfin.callbloker

/**
 * Pure phone-number-prefix logic, deliberately free of any Android dependency
 * so it can be unit-tested on the JVM. Both the UI and the call-screening
 * service go through here.
 */
object Prefixes {

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
