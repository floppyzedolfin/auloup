package com.floppyzedolfin.callbloker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "callbloker")

/** Single source of truth for the set of blocked phone-number prefixes. */
class PrefixRepository(private val context: Context) {

    private val key = stringSetPreferencesKey("blocked_prefixes")

    /** The stored prefixes, sorted, as a reactive stream for the UI. */
    val prefixes: Flow<List<String>> =
        context.dataStore.data.map { prefs -> prefs[key].orEmpty().sorted() }

    suspend fun add(raw: String) {
        val prefix = Prefixes.normalize(raw) ?: return
        context.dataStore.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() + prefix
        }
    }

    suspend fun remove(prefix: String) {
        context.dataStore.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() - prefix
        }
    }

    /** A one-shot read for the call-screening service. */
    suspend fun current(): Set<String> =
        context.dataStore.data.first()[key].orEmpty()
}
