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
import us.huseli.retain.Constants.DEFAULT_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_BASE_DIR
import us.huseli.retain.Constants.PREF_NEXTCLOUD_PASSWORD
import us.huseli.retain.Constants.PREF_NEXTCLOUD_URI
import us.huseli.retain.Constants.PREF_NEXTCLOUD_USERNAME
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.syncbackend.NextCloudEngine
import us.huseli.retain.syncbackend.tasks.TaskResult
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import javax.inject.Inject

@HiltViewModel
class NextCloudViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: SyncBackendRepository,
    private val engine: NextCloudEngine,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var dataChanged = false
    private val _baseDir = MutableStateFlow(
        preferences.getString(PREF_NEXTCLOUD_BASE_DIR, DEFAULT_NEXTCLOUD_BASE_DIR) ?: DEFAULT_NEXTCLOUD_BASE_DIR
    )
    private val _isAuthError = MutableStateFlow(false)
    private val _isUrlError = MutableStateFlow(false)
    private val _isWorking = MutableStateFlow<Boolean?>(null)
    private val _password = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_PASSWORD, "") ?: "")
    private val _uri = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_URI, "") ?: "")
    private val _username = MutableStateFlow(preferences.getString(PREF_NEXTCLOUD_USERNAME, "") ?: "")

    val baseDir = _baseDir.asStateFlow()
    val isAuthError = _isAuthError.asStateFlow()
    val isTesting = engine.isTesting
    val isUrlError = _isUrlError.asStateFlow()
    val isWorking = _isWorking.asStateFlow()
    val password = _password.asStateFlow()
    val uri = _uri.asStateFlow()
    val username = _username.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        repository.addOnSaveListener { save() }
    }

    private fun resetStatus() {
        _isAuthError.value = false
        _isUrlError.value = false
        _isWorking.value = null
    }

    private fun save() {
        preferences.edit()
            .putString(PREF_NEXTCLOUD_BASE_DIR, _baseDir.value)
            .putString(PREF_NEXTCLOUD_PASSWORD, _password.value)
            .putString(PREF_NEXTCLOUD_URI, _uri.value)
            .putString(PREF_NEXTCLOUD_USERNAME, _username.value)
            .apply()
        if (dataChanged) {
            repository.needsTesting.value = true
            dataChanged = false
        }
    }

    fun test(onResult: (TestTaskResult) -> Unit) = viewModelScope.launch {
        repository.needsTesting.value = false
        engine.test(
            uri = Uri.parse(_uri.value),
            username = _username.value,
            password = _password.value,
            baseDir = _baseDir.value,
        ) { result ->
            _isWorking.value = result.success
            _isUrlError.value =
                result.status == TaskResult.Status.UNKNOWN_HOST || result.status == TaskResult.Status.CONNECT_ERROR
            _isAuthError.value = result.status == TaskResult.Status.AUTH_ERROR
            onResult(result)
        }
    }

    fun updateField(field: String, value: Any) {
        when (field) {
            PREF_NEXTCLOUD_URI -> {
                if (value != _uri.value) {
                    _uri.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_NEXTCLOUD_USERNAME -> {
                if (value != _username.value) {
                    _username.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_NEXTCLOUD_PASSWORD -> {
                if (value != _password.value) {
                    _password.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_NEXTCLOUD_BASE_DIR -> _baseDir.value =
                preferences.getString(key, DEFAULT_NEXTCLOUD_BASE_DIR) ?: DEFAULT_NEXTCLOUD_BASE_DIR
            PREF_NEXTCLOUD_PASSWORD -> _password.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_URI -> _uri.value = preferences.getString(key, "") ?: ""
            PREF_NEXTCLOUD_USERNAME -> _username.value = preferences.getString(key, "") ?: ""
        }
    }
}
