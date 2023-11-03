package us.huseli.retain.viewmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import us.huseli.retain.Constants.DEFAULT_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_BASE_DIR
import us.huseli.retain.Constants.PREF_SFTP_HOSTNAME
import us.huseli.retain.Constants.PREF_SFTP_PASSWORD
import us.huseli.retain.Constants.PREF_SFTP_PORT
import us.huseli.retain.Constants.PREF_SFTP_USERNAME
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.syncbackend.SFTPEngine
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import javax.inject.Inject

@HiltViewModel
class SFTPViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val engine: SFTPEngine,
    private val repository: SyncBackendRepository,
) : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private var dataChanged = false
    private val _baseDir =
        MutableStateFlow(preferences.getString(PREF_SFTP_BASE_DIR, DEFAULT_SFTP_BASE_DIR) ?: DEFAULT_SFTP_BASE_DIR)
    private val _hostname = MutableStateFlow(preferences.getString(PREF_SFTP_HOSTNAME, "") ?: "")
    private val _isWorking = MutableStateFlow<Boolean?>(null)
    private val _password = MutableStateFlow(preferences.getString(PREF_SFTP_PASSWORD, "") ?: "")
    private val _port = MutableStateFlow(preferences.getInt(PREF_SFTP_PORT, 22))
    private val _username = MutableStateFlow(preferences.getString(PREF_SFTP_USERNAME, "") ?: "")

    val baseDir = _baseDir.asStateFlow()
    val hostname = _hostname.asStateFlow()
    val isTesting = engine.isTesting
    val isWorking = _isWorking.asStateFlow()
    val password = _password.asStateFlow()
    val port = _port.asStateFlow()
    val promptYesNo = engine.promptYesNo
    val username = _username.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
        repository.addOnSaveListener { save() }
    }

    fun approveKey() = engine.approveKey()

    fun denyKey() = engine.denyKey()

    private fun save() {
        preferences.edit()
            .putInt(PREF_SFTP_PORT, _port.value)
            .putString(PREF_SFTP_BASE_DIR, _baseDir.value)
            .putString(PREF_SFTP_HOSTNAME, _hostname.value)
            .putString(PREF_SFTP_PASSWORD, _password.value)
            .putString(PREF_SFTP_USERNAME, _username.value)
            .apply()
        if (dataChanged) {
            repository.needsTesting.value = true
            dataChanged = false
        }
    }

    fun test(onResult: (TestTaskResult) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        repository.needsTesting.value = false
        engine.test(
            hostname = _hostname.value,
            username = _username.value,
            password = _password.value,
            baseDir = _baseDir.value,
        ) { result ->
            _isWorking.value = result.success
            onResult(result)
        }
    }

    fun updateField(field: String, value: Any) {
        when (field) {
            PREF_SFTP_BASE_DIR -> {
                if (value != _baseDir.value) {
                    _baseDir.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_SFTP_HOSTNAME -> {
                if (value != _hostname.value) {
                    _hostname.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_SFTP_PASSWORD -> {
                if (value != _password.value) {
                    _password.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_SFTP_PORT -> {
                if (value != _port.value) {
                    _port.value = value as Int
                    dataChanged = true
                    resetStatus()
                }
            }
            PREF_SFTP_USERNAME -> {
                if (value != _username.value) {
                    _username.value = value as String
                    dataChanged = true
                    resetStatus()
                }
            }
        }
    }

    private fun resetStatus() {
        _isWorking.value = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            PREF_SFTP_BASE_DIR -> _baseDir.value =
                preferences.getString(key, DEFAULT_SFTP_BASE_DIR) ?: DEFAULT_SFTP_BASE_DIR
            PREF_SFTP_HOSTNAME -> _hostname.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PASSWORD -> _password.value = preferences.getString(key, "") ?: ""
            PREF_SFTP_PORT -> _port.value = preferences.getInt(key, 22)
            PREF_SFTP_USERNAME -> _username.value = preferences.getString(key, "") ?: ""
        }
    }
}
