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
     * True when [number] starts with any of the [prefixes]. Both sides are
     * normalized first, so "+1 (555)…" and "+1555…" compare equal.
     */
    fun isBlocked(number: String, prefixes: Set<String>): Boolean {
        val n = normalize(number) ?: return false
        return prefixes.any { prefix ->
            normalize(prefix)?.let { n.startsWith(it) } ?: false
        }
    }
}
