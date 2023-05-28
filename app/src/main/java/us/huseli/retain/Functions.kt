package us.huseli.retain

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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


@Composable
fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
)


fun cleanUri(value: String): String {
    val regex = Regex("^https?://.+")
    if (value.isBlank()) return ""
    if (!regex.matches(value)) return "https://$value".trimEnd('/')
    return value.trimEnd('/')
}


@Suppress("SameReturnValue")
fun fileToImageBitmap(file: File, context: Context): ImageBitmap? {
    if (file.isFile) {
        Uri.fromFile(file)?.let { uri ->
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            return bitmap?.asImageBitmap()
        }
    }
    return null
}
