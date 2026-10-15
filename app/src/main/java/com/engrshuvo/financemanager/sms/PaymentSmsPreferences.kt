package com.engrshuvo.financemanager.sms

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.paymentSmsDataStore: DataStore<Preferences> by preferencesDataStore(name = "payment_sms_preferences")

data class PaymentSmsPreferences(
    val isEnabled: Boolean = false,
    val activatedAt: Long = 0L,
    val lastImportedAt: Long = 0L,
    val importedCount: Int = 0,
    val autoPostUnambiguous: Boolean = false
)

class PaymentSmsPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_ENABLED = booleanPreferencesKey("sms_import_enabled")
        val ACTIVATED_AT = longPreferencesKey("sms_import_activated_at")
        val LAST_IMPORTED_AT = longPreferencesKey("sms_import_last_imported_at")
        val IMPORTED_COUNT = intPreferencesKey("sms_import_count")
        val AUTO_POST = booleanPreferencesKey("sms_import_auto_post")
    }

    val preferencesFlow: Flow<PaymentSmsPreferences> = context.paymentSmsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            PaymentSmsPreferences(
                isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: false,
                activatedAt = preferences[PreferencesKeys.ACTIVATED_AT] ?: 0L,
                lastImportedAt = preferences[PreferencesKeys.LAST_IMPORTED_AT] ?: 0L,
                importedCount = preferences[PreferencesKeys.IMPORTED_COUNT] ?: 0,
                autoPostUnambiguous = preferences[PreferencesKeys.AUTO_POST] ?: false
            )
        }

    suspend fun getPreferencesDirect(): PaymentSmsPreferences {
        return preferencesFlow.first()
    }

    suspend fun updateEnabled(enabled: Boolean) {
        val now = System.currentTimeMillis()
        context.paymentSmsDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = enabled
            if (enabled) {
                // Strict new-message policy: update activation timestamp to right now
                preferences[PreferencesKeys.ACTIVATED_AT] = now
            } else {
                preferences[PreferencesKeys.ACTIVATED_AT] = 0L
            }
        }
    }

    suspend fun recordImportSuccess(timestamp: Long = System.currentTimeMillis()) {
        context.paymentSmsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_IMPORTED_AT] = timestamp
            val current = preferences[PreferencesKeys.IMPORTED_COUNT] ?: 0
            preferences[PreferencesKeys.IMPORTED_COUNT] = current + 1
        }
    }

    suspend fun updateAutoPost(autoPost: Boolean) {
        context.paymentSmsDataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_POST] = autoPost
        }
    }
}
