package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.DEFAULT_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_MIN_COLUMN_WIDTH
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.nextcloud.tasks.TestNextCloudTaskResult
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: NoteRepository
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val nextCloudUri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
    val nextCloudUsername = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: "")
    val nextCloudPassword = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: "")
    val minColumnWidth = MutableStateFlow(preferences.getInt(PREF_MIN_COLUMN_WIDTH, DEFAULT_MIN_COLUMN_WIDTH))
    val isNextCloudTesting = MutableStateFlow(false)
    val isNextCloudWorking = MutableStateFlow<Boolean?>(null)
    val isNextCloudUrlFail = MutableStateFlow(false)
    val isNextCloudCredentialsFail = MutableStateFlow(false)
    val nextCloudNeedsTesting = repository.nextCloudNeedsTesting.asStateFlow()

    fun testNextCloud(onResult: (TestNextCloudTaskResult) -> Unit) = viewModelScope.launch {
        repository.nextCloudNeedsTesting.value = false
        isNextCloudTesting.value = true
        repository.testNextcloud(
            Uri.parse(nextCloudUri.value),
            nextCloudUsername.value,
            nextCloudPassword.value
        ) { result ->
            isNextCloudTesting.value = false
            isNextCloudWorking.value = result.success
            isNextCloudUrlFail.value = result.isUrlFail
            isNextCloudCredentialsFail.value = result.isCredentialsFail
            onResult(result)
        }
    }

    fun save() {
        preferences.edit()
            .putString(PREF_NEXTCLOUD_URI, nextCloudUri.value)
            .putString(PREF_NEXTCLOUD_USERNAME, nextCloudUsername.value)
            .putString(PREF_NEXTCLOUD_PASSWORD, nextCloudPassword.value)
            .putInt(PREF_MIN_COLUMN_WIDTH, minColumnWidth.value)
            .apply()
        repository.nextCloudNeedsTesting.value = true
    }

    val updateField: (field: String, value: Any) -> Unit = { field, value ->
        when (field) {
            PREF_NEXTCLOUD_URI -> nextCloudUri.value = value as String
            PREF_NEXTCLOUD_USERNAME -> nextCloudUsername.value = value as String
            PREF_NEXTCLOUD_PASSWORD -> nextCloudPassword.value = value as String
            PREF_MIN_COLUMN_WIDTH -> minColumnWidth.value = value as Int
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
