package us.huseli.retain.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import us.huseli.retain.repositories.NoteRepository
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(private val repository: NoteRepository) : ViewModel() {
    suspend fun getImageBitmap(filename: String) = repository.getImageBitmap(filename)
}
