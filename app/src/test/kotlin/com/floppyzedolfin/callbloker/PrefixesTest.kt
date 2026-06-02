package com.floppyzedolfin.callbloker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefixesTest {

    @Test
    fun normalize_stripsFormattingButKeepsLeadingPlus() {
        assertEquals("+1555", Prefixes.normalize(" +1 (555) "))
        assertEquals("0900", Prefixes.normalize("09-00"))
    }

    @Test
    fun normalize_returnsNullWhenNoDigits() {
        assertNull(Prefixes.normalize(""))
        assertNull(Prefixes.normalize("+"))
        assertNull(Prefixes.normalize("   "))
    }

    @Test
    fun isBlocked_matchesOnPrefix() {
        val prefixes = setOf("+1900", "0900")
        assertTrue(Prefixes.isBlocked("+1 900 555 0100", prefixes))
        assertTrue(Prefixes.isBlocked("0900123456", prefixes))
    }

    @Test
    fun isBlocked_doesNotMatchUnlistedNumbers() {
        val prefixes = setOf("+1900")
        assertFalse(Prefixes.isBlocked("+1 415 555 0100", prefixes))
    }

    @Test
    fun isBlocked_isFalseForEmptyInputs() {
        assertFalse(Prefixes.isBlocked("", setOf("+1900")))
        assertFalse(Prefixes.isBlocked("+1900123", emptySet()))
    }

    @Test
    fun longestMatch_picksTheMostSpecificPrefix() {
        val prefixes = setOf("+3316212", "+331621")
        // Both match, but the longer (more specific) one wins.
        assertEquals("+3316212", Prefixes.longestMatch("+33162123455", prefixes))
    }

    @Test
    fun longestMatch_isNullWhenNothingMatches() {
        assertNull(Prefixes.longestMatch("+14155550100", setOf("+3316212")))
        assertNull(Prefixes.longestMatch("", setOf("+33")))
    }

    @Test
    fun longestMatch_isIndependentOfInsertionOrder() {
        val number = "+33162123455"
        assertEquals("+3316212", Prefixes.longestMatch(number, listOf("+331621", "+3316212")))
        assertEquals("+3316212", Prefixes.longestMatch(number, listOf("+3316212", "+331621")))
    }

    @Test
    fun longestMatch_emptyNationalPrefixBlocksWholeCountryCode() {
        // "+33" with no national digits blocks any French number.
        assertEquals("+33", Prefixes.longestMatch("+33162123455", setOf("+33")))
    }

    @Test
    fun normalize_isIdempotent() {
        val once = Prefixes.normalize("+33 (1) 62-12")
        assertEquals(once, once?.let { Prefixes.normalize(it) })
    }
}
