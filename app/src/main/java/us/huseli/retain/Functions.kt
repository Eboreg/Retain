package us.huseli.retain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import us.huseli.retain.Constants.DEFAULT_MAX_IMAGE_DIMEN
import us.huseli.retain.Constants.IMAGE_SUBDIR
import us.huseli.retain.Constants.ZIP_BUFFER_SIZE
import us.huseli.retain.dataclasses.ImageData
import us.huseli.retaintheme.extensions.DateTimePrecision
import us.huseli.retaintheme.extensions.scaleToMaxSize
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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


fun String.cleanUri(): String {
    if (isBlank()) return ""
    if (!Regex("^https?://.+").matches(this)) return "https://$this".trimEnd('/')
    return trimEnd('/')
}


fun ZipFile.readTextFile(zipEntry: ZipEntry): String {
    return getInputStream(zipEntry).use { inputStream ->
        val buffer = ByteArray(ZIP_BUFFER_SIZE)
        val writer = StringWriter()
        var length: Int

        while (inputStream.read(buffer).also { length = it } > 0) {
            writer.write(buffer.decodeToString(0, length))
        }
        writer.toString()
    }
}


fun ZipFile.extractFile(zipEntry: ZipEntry, outfile: File) {
    val buffer = ByteArray(ZIP_BUFFER_SIZE)

    outfile.outputStream().use { outputStream ->
        getInputStream(zipEntry).use { inputStream ->
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
        }
    }
}


fun Context.uriToBitmap(uri: Uri): Bitmap? {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }

    return bitmap?.scaleToMaxSize(DEFAULT_MAX_IMAGE_DIMEN)
}


suspend fun uriToImageData(context: Context, uri: Uri): ImageData? {
    return context.uriToBitmap(uri)?.let { bitmap ->
        val imageDir = File(context.filesDir, IMAGE_SUBDIR).apply { mkdirs() }
        val basename = "${UUID.randomUUID()}.png"
        val imageFile = File(imageDir, basename)

        withContext(Dispatchers.IO) {
            imageFile.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

        ImageData(
            filename = basename,
            mimeType = "image/png",
            width = bitmap.width,
            height = bitmap.height,
            size = imageFile.length().toInt(),
        )
    }
}


fun isImageFile(filename: String): Boolean {
    return Files.probeContentType(Paths.get(filename))?.startsWith("image/") == true
}


fun File.toBitmap(): Bitmap? = takeIf { it.isFile }?.inputStream().use { BitmapFactory.decodeStream(it) }


fun <T> List<T>.switch(pos1: Int, pos2: Int): List<T> {
    val mutable = toMutableList()
    val item1 = this[pos1]
    val item2 = this[pos2]

    mutable[pos1] = item2
    mutable[pos2] = item1
    return mutable.toList()
}


fun Instant.isoTime(precision: DateTimePrecision = DateTimePrecision.SECOND): String {
    val pattern = when (precision) {
        DateTimePrecision.DAY -> throw Error()
        DateTimePrecision.HOUR -> "HH"
        DateTimePrecision.MINUTE -> "HH:mm"
        DateTimePrecision.SECOND -> "HH:mm:ss"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.systemDefault())

    return formatter.format(this)
}


object Logger : ILogger
