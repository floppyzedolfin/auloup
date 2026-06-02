package com.floppyzedolfin.callbloker

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/** A blocked prefix together with how many calls it has blocked so far. */
data class BlockedPrefix(val prefix: String, val blockedCount: Int)

/** The decision for one screened call. */
data class ScreenResult(val blocked: Boolean, val notify: Boolean)

private val Context.dataStore by preferencesDataStore(name = "callbloker")

/**
 * Single source of truth for the blocked prefixes (and their per-prefix block
 * counts) plus the "notify on block" preference.
 *
 * Prefixes are stored as a small JSON object, `{ "<prefix>": <count> }`, under
 * one key — keeping the schema in one place with no extra dependencies.
 */
class PrefixRepository(private val context: Context) {

    private val prefixesKey = stringPreferencesKey("blocked_prefixes")
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")

    /** Stored prefixes with their counts, sorted by prefix, as a stream for the UI. */
    val prefixes: Flow<List<BlockedPrefix>> =
        context.dataStore.data.map { prefs ->
            decode(prefs[prefixesKey])
                .map { BlockedPrefix(it.key, it.value) }
                .sortedBy { it.prefix }
        }

    /** Whether to show a notification when a call is blocked (default on). */
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: true }

    suspend fun add(raw: String) {
        val prefix = Prefixes.normalize(raw) ?: return
        context.dataStore.edit { prefs ->
            val counts = decode(prefs[prefixesKey]).toMutableMap()
            counts.putIfAbsent(prefix, 0)
            prefs[prefixesKey] = encode(counts)
        }
    }

    suspend fun remove(prefix: String) {
        context.dataStore.edit { prefs ->
            val counts = decode(prefs[prefixesKey]).toMutableMap()
            counts.remove(prefix)
            prefs[prefixesKey] = encode(counts)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsKey] = enabled }
    }

    /**
     * Screens [number] against the stored prefixes. If it matches, records a
     * block against the most specific (longest) matching prefix and reports
     * that the call should be blocked, along with whether to notify.
     */
    suspend fun screenAndRecord(number: String): ScreenResult {
        val prefs = context.dataStore.data.first()
        val notify = prefs[notificationsKey] ?: true
        val match = Prefixes.longestMatch(number, decode(prefs[prefixesKey]).keys)
            ?: return ScreenResult(blocked = false, notify = notify)

        context.dataStore.edit { p ->
            val counts = decode(p[prefixesKey]).toMutableMap()
            counts[match] = (counts[match] ?: 0) + 1
            p[prefixesKey] = encode(counts)
        }
        return ScreenResult(blocked = true, notify = notify)
    }

    private fun decode(json: String?): Map<String, Int> {
        if (json.isNullOrBlank()) return emptyMap()
        val obj = JSONObject(json)
        return buildMap {
            for (key in obj.keys()) put(key, obj.optInt(key, 0))
        }
    }

    private fun encode(counts: Map<String, Int>): String {
        val obj = JSONObject()
        for ((prefix, count) in counts) obj.put(prefix, count)
        return obj.toString()
    }
}
