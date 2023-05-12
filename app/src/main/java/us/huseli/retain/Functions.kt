package us.huseli.retain

import android.content.Context
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun copyFileToLocal(context: Context, uri: Uri, outFile: File): File {
    return withContext(Dispatchers.IO) {
        return@withContext runCatching {
            (context.contentResolver.openInputStream(uri) ?: run {
                throw Exception("copyFileToLocal: openInputStream returned null")
            }).use { inputStream ->
                outFile.createNewFile()
                FileOutputStream(outFile).use { outputStream ->
                    val buf = ByteArray(1024)
                    var len: Int
                    while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
                }
            }
            return@runCatching outFile
        }
    }.getOrElse { throw Exception("copyFileToLocal threw exception", it) }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun outlinedTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
)
