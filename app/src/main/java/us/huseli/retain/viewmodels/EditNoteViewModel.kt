package us.huseli.retain.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.graphics.scale
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.DEFAULT_MAX_IMAGE_DIMEN
import us.huseli.retain.Constants.NOTE_ID_SAVED_STATE_KEY
import us.huseli.retain.copyFileToLocal
import us.huseli.retain.data.NoteRepository
import us.huseli.retain.data.entities.Image
import us.huseli.retain.data.entities.ImageWithBitmap
import us.huseli.retain.data.entities.Note
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

@HiltViewModel
open class EditNoteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    protected val repository: NoteRepository
) : ViewModel() {
    private val _title = MutableStateFlow("")
    private val _text = MutableStateFlow("")
    private val _colorIdx = MutableStateFlow(0)

    protected val _showChecked = MutableStateFlow(true)
    protected var _isDirty = false
    protected var _isStored = false

    val noteId: UUID = UUID.fromString(savedStateHandle.get<String>(NOTE_ID_SAVED_STATE_KEY)!!)
    var note: Note? = null
    val title = _title.asStateFlow()
    val text = _text.asStateFlow()
    val colorIdx = _colorIdx.asStateFlow()
    val showChecked = _showChecked.asStateFlow()
    val imagesWithBitmap: Flow<List<ImageWithBitmap>> = repository.flowImagesWithBitmap(noteId)

    val shouldSave: Boolean
        get() = _isDirty || !_isStored

    protected suspend fun saveNote() {
        if (!_isStored || _isDirty) {
            repository.upsertNote(noteId, _title.value, _text.value, _showChecked.value, _colorIdx.value)
            _isStored = true
            _isDirty = false
        }
    }

    fun setColorIdx(value: Int) {
        if (value != _colorIdx.value) {
            _colorIdx.value = value
            _isDirty = true
        }
    }

    fun setText(value: String) {
        if (value != _text.value) {
            _text.value = value
            _isDirty = true
        }
    }

    fun setTitle(value: String) {
        if (value != _title.value) {
            _title.value = value
            _isDirty = true
        }
    }

    fun insertImage(uri: Uri, context: Context) = viewModelScope.launch {
        val width: Int?
        val height: Int?
        val basename: String
        val mimeType: String?
        val imageFile: File

        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        if (bitmap != null && (bitmap.width > DEFAULT_MAX_IMAGE_DIMEN || bitmap.height > DEFAULT_MAX_IMAGE_DIMEN)) {
            val factor = DEFAULT_MAX_IMAGE_DIMEN.toFloat() / max(bitmap.width, bitmap.height)
            width = (bitmap.width * factor).roundToInt()
            height = (bitmap.height * factor).roundToInt()
            basename = "${UUID.randomUUID()}.png"
            mimeType = "image/png"
            val resized = bitmap.scale(width = width, height = height)
            imageFile = File(repository.imageDir, basename)

            withContext(Dispatchers.IO) {
                FileOutputStream(imageFile).use { outputStream ->
                    resized.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
        } else {
            val extension = context.contentResolver.getType(uri)?.split("/")?.last()
            basename = UUID.randomUUID().toString() + if (extension != null) ".$extension" else ""
            mimeType = context.contentResolver.getType(uri)
            imageFile = File(repository.imageDir, basename)
            copyFileToLocal(context, uri, imageFile)
            width = bitmap?.width
            height = bitmap?.height
        }

        if (!_isStored) saveNote()
        repository.insertImage(noteId, basename, mimeType, width, height, imageFile.length().toInt())
        _isDirty = true
    }

    fun deleteImage(image: Image) = viewModelScope.launch {
        repository.deleteImage(image)
        _isDirty = true
    }

    init {
        viewModelScope.launch {
            repository.flowNote(noteId).transformWhile {
                if (it != null) emit(it)
                it == null
            }.collect {
                note = it
                _title.value = it.title
                _text.value = it.text
                _showChecked.value = it.showChecked
                _colorIdx.value = it.colorIdx
                _isStored = true
            }
        }
    }
}
