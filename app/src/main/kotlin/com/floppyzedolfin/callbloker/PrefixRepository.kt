package com.floppyzedolfin.callbloker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** A blocked prefix together with how many calls it has blocked so far. */
data class BlockedPrefix(val prefix: String, val blockedCount: Int)

/** One blocked call: the matched prefix, the caller's number, and when. */
data class BlockedCall(val prefix: String, val number: String, val timeMillis: Long)

/** The decision for one screened call. */
data class ScreenResult(val blocked: Boolean, val notify: Boolean)

private val Context.dataStore by preferencesDataStore(name = "callbloker")

/**
 * Single source of truth for the configured prefixes, the history of blocked
 * calls, and the "notify on block" preference.
 *
 * Both the prefix list and the call history are stored as small JSON values
 * under one key each — keeping the schema in one place with no extra
 * dependencies. Per-prefix counts are derived from the history, so they can
 * never drift from it.
 */
class PrefixRepository(private val context: Context) {

    private val prefixesKey = stringPreferencesKey("prefixes")
    private val historyKey = stringPreferencesKey("history")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")

    /** Configured prefixes with their derived block counts, sorted by prefix. */
    val prefixes: Flow<List<BlockedPrefix>> =
        context.dataStore.data.map { prefs ->
            val counts = decodeHistory(prefs[historyKey]).groupingBy { it.prefix }.eachCount()
            decodePrefixes(prefs[prefixesKey])
                .map { BlockedPrefix(it, counts[it] ?: 0) }
                .sortedBy { it.prefix }
        }

    /** Whether to show a notification when a call is blocked (default on). */
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: true }

    /** Blocked calls for [prefix], most recent first. */
    fun history(prefix: String): Flow<List<BlockedCall>> =
        context.dataStore.data.map { prefs ->
            decodeHistory(prefs[historyKey])
                .filter { it.prefix == prefix }
                .sortedByDescending { it.timeMillis }
        }

    suspend fun add(raw: String) {
        val prefix = Prefixes.normalize(raw) ?: return
        context.dataStore.edit { prefs ->
            val set = decodePrefixes(prefs[prefixesKey]).toMutableSet()
            set.add(prefix)
            prefs[prefixesKey] = encodePrefixes(set)
        }
    }

    /** Removes a prefix and forgets its blocked-call history. */
    suspend fun remove(prefix: String) {
        context.dataStore.edit { prefs ->
            val set = decodePrefixes(prefs[prefixesKey]).toMutableSet().apply { remove(prefix) }
            prefs[prefixesKey] = encodePrefixes(set)
            val history = decodeHistory(prefs[historyKey]).filterNot { it.prefix == prefix }
            prefs[historyKey] = encodeHistory(history)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsKey] = enabled }
    }

    /**
     * Screens [number] against the stored prefixes. If it matches, records the
     * call against the most specific (longest) matching prefix and reports that
     * the call should be blocked, along with whether to notify.
     */
    suspend fun screenAndRecord(number: String, timeMillis: Long): ScreenResult {
        val prefs = context.dataStore.data.first()
        val notify = prefs[notificationsKey] ?: true
        val match = Prefixes.longestMatch(number, decodePrefixes(prefs[prefixesKey]))
            ?: return ScreenResult(blocked = false, notify = notify)

        context.dataStore.edit { p ->
            val history = decodeHistory(p[historyKey]).toMutableList()
            history.add(BlockedCall(match, number, timeMillis))
            p[historyKey] = encodeHistory(history)
        }
        return ScreenResult(blocked = true, notify = notify)
    }

    private fun decodePrefixes(json: String?): Set<String> {
        if (json.isNullOrBlank()) return emptySet()
        val array = JSONArray(json)
        return buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
    }

    private fun encodePrefixes(prefixes: Set<String>): String {
        val array = JSONArray()
        prefixes.forEach { array.put(it) }
        return array.toString()
    }

    private fun decodeHistory(json: String?): List<BlockedCall> {
        if (json.isNullOrBlank()) return emptyList()
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(BlockedCall(o.getString("p"), o.getString("n"), o.getLong("t")))
            }
        }
    }

    private fun encodeHistory(calls: List<BlockedCall>): String {
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
}
