package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tariqDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tariq_halal_prefs"
)

data class LocalSessionPrefs(
    val selectedRoleKey: String = "",
    val sanctuaryId: String = "",
    val darkModeEnabled: Boolean = false,
    val lastAuthUid: String = ""
)

class LocalPreferences(private val context: Context) {

    private object Keys {
        val SELECTED_ROLE = stringPreferencesKey("selected_role")
        val SANCTUARY_ID = stringPreferencesKey("sanctuary_id")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val LAST_AUTH_UID = stringPreferencesKey("last_auth_uid")
    }

    val sessionFlow: Flow<LocalSessionPrefs> = context.tariqDataStore.data.map { prefs ->
        LocalSessionPrefs(
            selectedRoleKey = prefs[Keys.SELECTED_ROLE] ?: "",
            sanctuaryId = prefs[Keys.SANCTUARY_ID] ?: "",
            darkModeEnabled = prefs[Keys.DARK_MODE] ?: false,
            lastAuthUid = prefs[Keys.LAST_AUTH_UID] ?: ""
        )
    }

    suspend fun saveSession(uid: String, roleKey: String, sanctuaryId: String) {
        context.tariqDataStore.edit { prefs ->
            prefs[Keys.LAST_AUTH_UID] = uid
            prefs[Keys.SELECTED_ROLE] = roleKey
            prefs[Keys.SANCTUARY_ID] = sanctuaryId
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.tariqDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun clearRoleSession() {
        context.tariqDataStore.edit { prefs ->
            prefs.remove(Keys.SELECTED_ROLE)
            prefs.remove(Keys.SANCTUARY_ID)
            prefs.remove(Keys.LAST_AUTH_UID)
        }
    }
}
