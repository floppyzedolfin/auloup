package com.floppyzedolfin.auloup.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/**
 * Pure (de)serialisation and aggregation for the stored prefixes and blocked-call
 * history. Android-free: [PrefixRepository] owns the DataStore I/O and delegates
 * the JSON and counting here, so this logic is unit-testable on the JVM.
 *
 * Storage shape: prefixes are a JSON `{ "<prefix>": <isOfficial> }` object; the
 * history is a JSON array of `{ "p": prefix, "n": number, "t": timeMillis }`.
 */
object PrefixData {

    /**
     * Decodes the stored prefixes as prefix -> isOfficial. Also migrates the old
     * format (a plain JSON array of prefixes) by treating them all as user-added.
     */
    fun decodePrefixes(json: String?): Map<String, Boolean> {
        if (json.isNullOrBlank()) return emptyMap()
        return if (json.trimStart().startsWith("[")) {
            val array = JSONArray(json)
            buildMap { for (i in 0 until array.length()) put(array.getString(i), false) }
        } else {
            val obj = JSONObject(json)
            buildMap { for (key in obj.keys()) put(key, obj.optBoolean(key, false)) }
        }
    }

    fun encodePrefixes(prefixes: Map<String, Boolean>): String {
        val obj = JSONObject()
        for ((prefix, official) in prefixes) obj.put(prefix, official)
        return obj.toString()
    }

    fun decodeHistory(json: String?): List<BlockedCall> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(BlockedCall(o.getString("p"), o.getString("n"), o.getLong("t")))
            }
        }
    }

    fun encodeHistory(calls: List<BlockedCall>): String {
        val array = JSONArray()
        calls.forEach { call ->
            array.put(
                JSONObject()
                    .put("p", call.prefix)
                    .put("n", call.number)
                    .put("t", call.timeMillis),
            )
        }
        return array.toString()
    }

    /** How many calls each prefix has blocked, derived from [calls]. */
    fun countsByPrefix(calls: List<BlockedCall>): Map<String, Int> =
        calls.groupingBy { it.prefix }.eachCount()

    /** How many of [calls] fall on the same calendar day as [timeMillis] in [zone]. */
    fun blockedOnDayOf(calls: List<BlockedCall>, timeMillis: Long, zone: ZoneId): Int {
        val day = Instant.ofEpochMilli(timeMillis).atZone(zone).toLocalDate()
        return calls.count { Instant.ofEpochMilli(it.timeMillis).atZone(zone).toLocalDate() == day }
    }
}
