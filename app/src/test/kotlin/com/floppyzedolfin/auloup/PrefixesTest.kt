package com.floppyzedolfin.auloup

import com.floppyzedolfin.auloup.telephony.Prefixes
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
    fun longestMatch_matchesAnyStoredPrefixLength() {
        // longestMatch itself is length-agnostic (entry enforces a 3-digit minimum).
        assertEquals("+33", Prefixes.longestMatch("+33162123455", setOf("+33")))
    }

    @Test
    fun normalize_isIdempotent() {
        val once = Prefixes.normalize("+33 (1) 62-12")
        assertEquals(once, once?.let { Prefixes.normalize(it) })
    }

    @Test
    fun nationalDigits_stripsTrunkPrefix() {
        assertEquals("160", Prefixes.nationalDigits("01 60", "0")) // France trunk 0
        assertEquals("160", Prefixes.nationalDigits("160", "0")) // already trunk-free
        assertEquals("06", Prefixes.nationalDigits("06", "")) // Italy keeps the 0
        assertEquals("415", Prefixes.nationalDigits("1415", "1")) // NANP trunk 1
    }

    @Test
    fun buildPrefix_stripsTrunkAndRequiresThreeDigits() {
        assertEquals("+33160", Prefixes.buildPrefix(33, "0", "01 60")) // 01 60 == +33 1 60
        assertEquals("+33160", Prefixes.buildPrefix(33, "0", "160"))
        assertEquals("+447911", Prefixes.buildPrefix(44, "0", "07911")) // UK 07911 == +44 7911
        assertEquals("+1415", Prefixes.buildPrefix(1, "1", "1415"))
        assertEquals("+39061", Prefixes.buildPrefix(39, "", "061")) // Italy keeps the 0
    }

    @Test
    fun buildPrefix_rejectsFewerThanThreeNationalDigits() {
        assertNull(Prefixes.buildPrefix(33, "0", "01")) // only "1" after trunk
        assertNull(Prefixes.buildPrefix(33, "0", "")) // whole country code no longer allowed
        assertNull(Prefixes.buildPrefix(39, "", "06")) // 2 digits
    }

    @Test
    fun toInternational_handlesAllFormats() {
        // Already international.
        assertEquals("+33162123455", Prefixes.toInternational("+33 162 123455", 33, "0"))
        // National (French) using the device region.
        assertEquals("+33160123456", Prefixes.toInternational("0160123456", 33, "0"))
        // National (UK).
        assertEquals("+447911123456", Prefixes.toInternational("07911123456", 44, "0"))
        // International-access 00 prefix.
        assertEquals("+33160", Prefixes.toInternational("0033160", 33, "0"))
        // NANP national with trunk 1.
        assertEquals("+14155550123", Prefixes.toInternational("14155550123", 1, "1"))
        assertNull(Prefixes.toInternational("", 33, "0"))
    }

    @Test
    fun nationalCallMatchesInternationalPrefix() {
        // A user blocks +33160 (entered as "01 60"); a national-format call matches.
        val stored = setOf(Prefixes.buildPrefix(33, "0", "0160")!!)
        val incoming = Prefixes.toInternational("0160123456", 33, "0")!!
        assertEquals("+33160", Prefixes.longestMatch(incoming, stored))
    }

    @Test
    fun frenchSimBlocksNationalCallForRegisteredPrefix() {
        // User registers "+33 1 62" (France selected, national part "1 62").
        val registered = Prefixes.buildPrefix(33, "0", "1 62")
        assertEquals("+33162", registered)
        val stored = setOf(registered!!)

        // French SIM region (code 33, trunk "0"). Caller ID may arrive in any form:
        val national = Prefixes.toInternational("01 62 44 68 21", 33, "0")
        val international = Prefixes.toInternational("+33 1 62 44 68 21", 33, "0")
        val withAccessCode = Prefixes.toInternational("0033162446821", 33, "0")
        assertEquals("+33162446821", national)
        assertEquals("+33162446821", international)
        assertEquals("+33162446821", withAccessCode)

        // All forms match the registered prefix.
        assertEquals("+33162", Prefixes.longestMatch(national!!, stored))
        assertEquals("+33162", Prefixes.longestMatch(international!!, stored))
        assertEquals("+33162", Prefixes.longestMatch(withAccessCode!!, stored))
    }
}
