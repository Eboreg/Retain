package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import us.huseli.retain.repositories.SyncBackendRepository
import us.huseli.retain.syncbackend.DropboxEngine
import us.huseli.retain.syncbackend.tasks.TestTaskResult
import javax.inject.Inject

@HiltViewModel
class DropboxViewModel @Inject constructor(
    private val engine: DropboxEngine,
    private val repository: SyncBackendRepository,
) : ViewModel() {
    private val _isWorking = MutableStateFlow<Boolean?>(null)

    val accountEmail = engine.accountEmail
    val isAuthenticated = engine.isAuthenticated
    val isTesting = engine.isTesting
    val isWorking = _isWorking.asStateFlow()

    fun authenticate() = engine.authenticate()

    fun revoke() = engine.revoke()

    fun test(onResult: (TestTaskResult) -> Unit) {
        _isWorking.value = null
        repository.needsTesting.value = false
        engine.test { result ->
            _isWorking.value = result.success
            onResult(result)
        }
    }
}
