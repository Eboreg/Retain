package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import us.huseli.retain.Constants.DEFAULT_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val nextCloudUri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
    val nextCloudUsername = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: "")
    val nextCloudPassword = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: "")
    val minColumnWidth = MutableStateFlow(preferences.getInt(PREF_MIN_COLUMN_WIDTH, DEFAULT_MIN_COLUMN_WIDTH))

    private fun cleanUri(value: String): String {
        val regex = Regex("^https?://.+")
        if (value.isBlank()) return ""
        if (!regex.matches(value)) return "https://$value".trimEnd('/')
        return value.trimEnd('/')
    }

    fun save(field: String) {
        when (field) {
            PREF_NEXTCLOUD_URI -> preferences.edit().putString(field, cleanUri(nextCloudUri.value)).apply()
            PREF_NEXTCLOUD_USERNAME -> preferences.edit().putString(field, nextCloudUsername.value).apply()
            PREF_NEXTCLOUD_PASSWORD -> preferences.edit().putString(field, nextCloudPassword.value).apply()
            PREF_MIN_COLUMN_WIDTH -> preferences.edit().putInt(field, minColumnWidth.value).apply()
        }
    }

    fun saveAll() {
        preferences.edit()
            .putString(PREF_NEXTCLOUD_URI, cleanUri(nextCloudUri.value))
            .putString(PREF_NEXTCLOUD_USERNAME, nextCloudUsername.value)
            .putString(PREF_NEXTCLOUD_PASSWORD, nextCloudPassword.value)
            .putInt(PREF_MIN_COLUMN_WIDTH, minColumnWidth.value)
            .apply()
    }

    val setString: (field: String, value: String) -> Unit = { field, value ->
        when (field) {
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = value
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = value
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = value
        }
    }

    val setInt: (field: String, value: Int) -> Unit = { field, value ->
        when (field) {
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = value
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = preferences.getString(key, "") ?: ""
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = preferences.getInt(key, DEFAULT_MIN_COLUMN_WIDTH)
        }
    }
}
