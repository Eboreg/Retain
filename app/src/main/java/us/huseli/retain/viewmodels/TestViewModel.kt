package us.huseli.retain.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import us.huseli.retaintheme.utils.AbstractBaseViewModel
import javax.inject.Inject
import kotlin.collections.plus

class TestClass(text: String) {
    var text by mutableStateOf(text)

    fun copy() = TestClass(text = text)
}

@HiltViewModel
class TestViewModel @Inject constructor() : AbstractBaseViewModel() {
    data class UndoState(val testObjects: List<TestClass>)

    val testObjects = MutableStateFlow<List<TestClass>>(emptyList())
    val undoStates = MutableStateFlow<List<UndoState>>(emptyList())
    val undoStateIdx = MutableStateFlow<Int?>(null)

    val isRedoPossible = combine(undoStates, undoStateIdx) { states, idx -> idx != null && idx < states.lastIndex }
        .stateWhileSubscribed(false)
    val isUndoPossible = undoStateIdx.map { it != null && it > 0 }.stateWhileSubscribed(false)

    init {
        saveUndoState()
    }

    fun addObject() {
        testObjects.value += TestClass("")
    }

    fun applyUndoState(idx: Int) {
        testObjects.value = undoStates.value[idx].testObjects
        undoStateIdx.value = idx
    }

    fun redo() {
        undoStateIdx.value?.also {
            if (it + 1 > -1 && it + 1 <= undoStates.value.lastIndex) applyUndoState(it + 1)
        }
    }

    fun saveUndoState() {
        undoStateIdx.value = (undoStateIdx.value ?: -1) + 1
        undoStates.value += UndoState(testObjects = testObjects.value.map { it.copy() })
    }

    fun undo() {
        undoStateIdx.value?.also {
            if (it - 1 > -1 && it - 1 <= undoStates.value.lastIndex) applyUndoState(it - 1)
        }
    }
}
