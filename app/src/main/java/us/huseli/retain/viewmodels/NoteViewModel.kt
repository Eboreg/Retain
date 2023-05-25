package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.BitmapImage
import us.huseli.retain.data.entities.NoteCombo
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val repository: NoteRepository,
    override val logger: Logger
) : ViewModel(), LogInterface {
    private val _selectedCombos = MutableStateFlow<Set<NoteCombo>>(emptySet())
    private val _trashedCombos = MutableStateFlow<List<NoteCombo>>(emptyList())
    private val _combos = MutableStateFlow<List<NoteCombo>>(emptyList())

    val combos = _combos.asStateFlow()
    val bitmapImages: Flow<List<BitmapImage>> = repository.bitmapImages
    val trashedNotes = _trashedCombos.asStateFlow()

    val selectedNotes: Flow<Set<NoteCombo>> = combine(_combos, _selectedCombos) { combos, selectedNotes ->
        selectedNotes.intersect(combos.toSet())
    }

    init {
        viewModelScope.launch {
            repository.combos.collect { combos ->
                _combos.value = combos
            }
        }
    }

    fun clearTrashNotes() {
        _trashedCombos.value = emptyList()
    }

    fun deselectAllNotes() {
        _selectedCombos.value = emptySet()
    }

    fun deselectNote(combo: NoteCombo) {
        _selectedCombos.value -= combo
        log("deselectNote: note=$combo, _selectedNotes.value=${_selectedCombos.value}")
    }

    fun getNoteBitmapImages(noteId: UUID): Flow<List<BitmapImage>> =
        repository.bitmapImages.map { list -> list.filter { it.image.noteId == noteId } }

    fun saveCombo(combo: NoteCombo) = viewModelScope.launch {
        repository.upsertNoteCombo(combo)
    }

    @Suppress("Destructure")
    fun saveNotePositions() = viewModelScope.launch {
        repository.updateNotePositions(
            _combos.value.mapIndexedNotNull { index, combo ->
                if (combo.note.position != index) combo.note.copy(position = index) else null
            }
        )
    }

    fun selectNote(combo: NoteCombo) {
        _selectedCombos.value += combo
        log("selectNote: note=$combo, _selectedNotes.value=${_selectedCombos.value}")
    }

    fun switchNotePositions(from: ItemPosition, to: ItemPosition) {
        _combos.value = _combos.value.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    fun trashNotes(notes: Collection<NoteCombo>) {
        log("trashNotes: $notes")
        _trashedCombos.value = notes.toList()
        viewModelScope.launch {
            repository.trashNotes(notes)
        }
    }

    fun undoTrashNotes() = viewModelScope.launch {
        repository.upsertNotes(_trashedCombos.value)
        clearTrashNotes()
    }
}
