package us.huseli.retain

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
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
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.ZIP_BUFFER_SIZE
import us.huseli.retain.data.entities.Image
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.roundToInt

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


fun readTextFileFromZip(zipFile: ZipFile, zipEntry: ZipEntry): String {
    val inputStream = zipFile.getInputStream(zipEntry)
    val buffer = ByteArray(ZIP_BUFFER_SIZE)
    val writer = StringWriter()
    var length: Int
    while (inputStream.read(buffer).also { length = it } > 0) {
        writer.write(buffer.decodeToString(0, length))
    }
    return writer.toString()
}


fun extractFileFromZip(zipFile: ZipFile, zipEntry: ZipEntry, outfile: File) {
    val buffer = ByteArray(ZIP_BUFFER_SIZE)

    FileOutputStream(outfile).use { outputStream ->
        val inputStream = zipFile.getInputStream(zipEntry)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }
    }
}


suspend fun uriToImage(context: Context, uri: Uri, noteId: UUID): Image? {
    val basename: String
    val mimeType: String?
    val imageFile: File
    val finalBitmap: Bitmap?

    val imageDir = File(context.filesDir, Constants.IMAGE_SUBDIR).apply { mkdirs() }
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    if (
        bitmap != null &&
        (bitmap.width > Constants.DEFAULT_MAX_IMAGE_DIMEN || bitmap.height > Constants.DEFAULT_MAX_IMAGE_DIMEN)
    ) {
        val factor = Constants.DEFAULT_MAX_IMAGE_DIMEN.toFloat() / max(bitmap.width, bitmap.height)
        val width = (bitmap.width * factor).roundToInt()
        val height = (bitmap.height * factor).roundToInt()
        basename = "${UUID.randomUUID()}.png"
        mimeType = "image/png"
        finalBitmap = bitmap.scale(width = width, height = height)
        imageFile = File(imageDir, basename)

        withContext(Dispatchers.IO) {
            FileOutputStream(imageFile).use { outputStream ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }
    } else {
        mimeType = uri.getMimeType(context)
        val extension = mimeType?.split('/')?.last() ?: uri.lastPathSegment?.split('.')?.last()
        basename = UUID.randomUUID().toString() + if (extension != null) ".$extension" else ""
        imageFile = File(imageDir, basename)
        copyFileToLocal(context, uri, imageFile)
        finalBitmap = bitmap
    }

    if (finalBitmap != null) {
        return Image(
            filename = basename,
            mimeType = mimeType,
            width = finalBitmap.width,
            height = finalBitmap.height,
            size = imageFile.length().toInt(),
            noteId = noteId,
        )
    }
    return null
}


fun isImageFile(filename: String): Boolean {
    return Files.probeContentType(Paths.get(filename))?.startsWith("image/") == true
}


fun Uri.getMimeType(context: Context): String? =
    if (scheme == ContentResolver.SCHEME_CONTENT) context.contentResolver.getType(this)
    else Files.probeContentType(Paths.get(path))
