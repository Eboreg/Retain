package us.huseli.retain.viewmodels

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import us.huseli.retain.Constants
import us.huseli.retain.Constants.NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID
import us.huseli.retain.LogInterface
import us.huseli.retain.Logger
import us.huseli.retain.dataclasses.entities.Image
import us.huseli.retain.repositories.NoteRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImageCarouselViewModel @Inject constructor(
    repository: NoteRepository,
    savedStateHandle: SavedStateHandle,
    override val logger: Logger,
) : ViewModel(), LogInterface {
    private val _noteId = UUID.fromString(savedStateHandle.get<String>(Constants.NAV_ARG_NOTE_ID)!!)
    private val _startImageId = savedStateHandle.get<String>(NAV_ARG_IMAGE_CAROUSEL_CURRENT_ID)!!
    private val _images = MutableStateFlow<List<Image>>(emptyList())
    private val _currentImage = MutableStateFlow<Image?>(null)

    val images = _images.asStateFlow()
    val currentImage = _currentImage.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentImageBitmap: Flow<ImageBitmap?> = _currentImage.flatMapLatest { image ->
        image?.let { repository.getImageBitmap(it.filename) } ?: emptyFlow()
    }

    init {
        viewModelScope.launch {
            _images.value = repository.listImagesByNoteId(_noteId)
            _currentImage.value = _images.value.find { it.filename == _startImageId }
        }
    }

    fun gotoNext() {
        val currentImageIdx = _images.value.indexOf(_currentImage.value)
        val newImageIdx = if (currentImageIdx >= _images.value.size - 1) 0 else currentImageIdx + 1
        _currentImage.value = _images.value[newImageIdx]
    }

    fun gotoPrevious() {
        val currentImageIdx = _images.value.indexOf(_currentImage.value)
        val newImageIdx = if (currentImageIdx == 0) _images.value.size - 1 else currentImageIdx - 1
        _currentImage.value = _images.value[newImageIdx]
    }
}
