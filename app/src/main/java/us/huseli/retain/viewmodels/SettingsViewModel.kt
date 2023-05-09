package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import us.huseli.retain.R
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val defaultMinColumnWidth = context.resources.getInteger(R.integer.defaultMinColumnWidthDp)

    val nextCloudUri = MutableStateFlow(preferences.getString("nextCloudUri", "") ?: "")
    val nextCloudUsername = MutableStateFlow(preferences.getString("nextCloudUsername", "") ?: "")
    val nextCloudPassword = MutableStateFlow(preferences.getString("nextCloudPassword", "") ?: "")
    val minColumnWidth = MutableStateFlow(preferences.getInt("minColumnWidth", defaultMinColumnWidth))

    private fun cleanUri(value: String): String {
        val regex = Regex("^https?://.+")
        if (value.isBlank()) return ""
        if (!regex.matches(value)) return "https://$value".trimEnd('/')
        return value.trimEnd('/')
    }

    fun save(field: String) {
        when (field) {
            "nextCloudUri" -> preferences.edit().putString(field, cleanUri(nextCloudUri.value)).apply()
            "nextCloudUsername" -> preferences.edit().putString(field, nextCloudUsername.value).apply()
            "nextCloudPassword" -> preferences.edit().putString(field, nextCloudPassword.value).apply()
            "minColumnWidth" -> preferences.edit().putInt(field, minColumnWidth.value).apply()
        }
    }

    fun saveAll() {
        preferences.edit()
            .putString("nextCloudUri", cleanUri(nextCloudUri.value))
            .putString("nextCloudUsername", nextCloudUsername.value)
            .putString("nextCloudPassword", nextCloudPassword.value)
            .putInt("minColumnWidth", minColumnWidth.value)
            .apply()
    }

    val setString: (field: String, value: String) -> Unit = { field, value ->
        when (field) {
            "nextCloudUri" -> nextCloudUri.value = value
            "nextCloudUsername" -> nextCloudUsername.value = value
            "nextCloudPassword" -> nextCloudPassword.value = value
        }
    }

    val setInt: (field: String, value: Int) -> Unit = { field, value ->
        when (field) {
            "minColumnWidth" -> minColumnWidth.value = value
        }
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "nextCloudUri" -> nextCloudUri.value = preferences.getString(key, "") ?: ""
            "nextCloudUsername" -> nextCloudUsername.value = preferences.getString(key, "") ?: ""
            "nextCloudPassword" -> nextCloudPassword.value = preferences.getString(key, "") ?: ""
            "minColumnWidth" -> minColumnWidth.value = preferences.getInt(key, defaultMinColumnWidth)
        }
    }
}
