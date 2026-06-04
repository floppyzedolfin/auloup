package com.floppyzedolfin.auloup

import com.floppyzedolfin.auloup.data.BlockedCall
import com.floppyzedolfin.auloup.data.PrefixData
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class PrefixDataTest {

    private val utc = ZoneOffset.UTC

    private fun ms(y: Int, mo: Int, d: Int, h: Int): Long =
        LocalDateTime.of(y, mo, d, h, 0).toInstant(utc).toEpochMilli()

    @Test
    fun prefixes_roundTripPreservesTheOfficialFlag() {
        val map = mapOf("+1900" to false, "+33162" to true)
        assertEquals(map, PrefixData.decodePrefixes(PrefixData.encodePrefixes(map)))
    }

    @Test
    fun prefixes_blankOrNullDecodesToEmpty() {
        assertEquals(emptyMap<String, Boolean>(), PrefixData.decodePrefixes(null))
        assertEquals(emptyMap<String, Boolean>(), PrefixData.decodePrefixes(""))
    }

    @Test
    fun prefixes_migratesLegacyArrayFormatAsUserAdded() {
        val decoded = PrefixData.decodePrefixes("""["+1900","+33162"]""")
        assertEquals(mapOf("+1900" to false, "+33162" to false), decoded)
    }

    @Test
    fun history_roundTrips() {
        val calls = listOf(
            BlockedCall("+33162", "+33162446821", 1000L),
            BlockedCall("+1900", "", 2000L),
        )
        assertEquals(calls, PrefixData.decodeHistory(PrefixData.encodeHistory(calls)))
    }

    @Test
    fun history_blankOrNullDecodesToEmpty() {
        assertEquals(emptyList<BlockedCall>(), PrefixData.decodeHistory(null))
        assertEquals(emptyList<BlockedCall>(), PrefixData.decodeHistory(""))
    }

    @Test
    fun countsByPrefix_talliesEachPrefix() {
        val calls = listOf(
            BlockedCall("+33162", "", 1),
            BlockedCall("+33162", "", 2),
            BlockedCall("+1900", "", 3),
        )
        assertEquals(mapOf("+33162" to 2, "+1900" to 1), PrefixData.countsByPrefix(calls))
    }

    @Test
    fun blockedOnDayOf_countsOnlyTheSameCalendarDay() {
        val calls = listOf(ms(2026, 6, 2, 10), ms(2026, 6, 2, 20), ms(2026, 6, 1, 23))
            .map { BlockedCall("+1", "", it) }
        assertEquals(2, PrefixData.blockedOnDayOf(calls, ms(2026, 6, 2, 12), utc))
    }
}
