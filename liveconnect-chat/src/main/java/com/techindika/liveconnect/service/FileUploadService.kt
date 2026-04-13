package com.techindika.liveconnect.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.techindika.liveconnect.model.Attachment
import com.techindika.liveconnect.model.AttachmentType
import com.techindika.liveconnect.network.ApiConstants
import com.techindika.liveconnect.network.ApiResult
import com.techindika.liveconnect.network.RetrofitClient
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okio.buffer
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Handles file uploads to the LiveConnect API.
 */
internal object FileUploadService {

    private const val TAG = "LiveConnect.Upload"

    /**
     * Upload a file to the server.
     *
     * @param context Application context.
     * @param widgetKey Widget key.
     * @param uri File URI from the file picker.
     * @param onProgress Progress callback (0-100).
     * @return ApiResult with file URL on success.
     */
    suspend fun upload(
        context: Context,
        widgetKey: String,
        uri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): ApiResult<UploadResult> {
        try {
            // Copy URI content to a temp file
            val fileInfo = getFileInfo(context, uri) ?: return ApiResult.failure("Could not read file")
            val tempFile = copyToTempFile(context, uri, fileInfo.name)
                ?: return ApiResult.failure("Could not create temp file")

            // Validate size
            if (tempFile.length() > Attachment.MAX_FILE_SIZE) {
                tempFile.delete()
                return ApiResult.failure("File size exceeds 10 MB limit")
            }

            val mimeType = fileInfo.mimeType ?: "application/octet-stream"
            val mediaType = mimeType.toMediaType()

            // Create progress-tracking request body
            val requestBody = ProgressRequestBody(
                tempFile.asRequestBody(mediaType),
                onProgress
            )

            val multipartBody = MultipartBody.Part.createFormData(
                "file", fileInfo.name, requestBody
            )

            val response = RetrofitClient.apiService.uploadFile(
                widgetKey = widgetKey,
                file = multipartBody,
                widgetKeyHeader = widgetKey
            )

            tempFile.delete()

            val json = JSONObject(response.string())
            val status = json.optString("status", "")
            if (status == "success") {
                val data = json.optJSONObject("data")
                val fileUrl = data?.optString("fileUrl", "") ?: ""
                val fileType = data?.optString("fileType", "document") ?: "document"
                return ApiResult.success(
                    UploadResult(fileUrl = fileUrl, fileType = fileType, fileName = fileInfo.name)
                )
            } else {
                return ApiResult.failure(json.optString("message", "Upload failed"))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Upload failed: ${e.message}")
            return ApiResult.failure(e.message ?: "Upload failed")
        }
    }

    /** Create an Attachment from a file URI (before upload). */
    fun createAttachmentFromUri(context: Context, uri: Uri): Attachment? {
        val info = getFileInfo(context, uri) ?: return null
        val mimeType = info.mimeType ?: "application/octet-stream"
        val type = if (mimeType.startsWith("image/") || mimeType.startsWith("video/")) {
            AttachmentType.MEDIA
        } else {
            AttachmentType.DOCUMENT
        }
        return Attachment(
            filename = info.name,
            filePath = uri.toString(),
            size = info.size,
            type = type,
            mimeType = mimeType
        )
    }

    private fun getFileInfo(context: Context, uri: Uri): FileInfo? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "file"
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    val mimeType = context.contentResolver.getType(uri)
                        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            name.substringAfterLast('.', "")
                        )
                    FileInfo(name, size, mimeType)
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun copyToTempFile(context: Context, uri: Uri, name: String): File? {
        return try {
            val tempFile = File(context.cacheDir, "lc_upload_$name")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }

    data class FileInfo(val name: String, val size: Long, val mimeType: String?)
    data class UploadResult(val fileUrl: String, val fileType: String, val fileName: String)

    /**
     * OkHttp RequestBody wrapper that reports upload progress.
     */
    private class ProgressRequestBody(
        private val delegate: RequestBody,
        private val onProgress: ((Int) -> Unit)?
    ) : RequestBody() {

        override fun contentType(): MediaType? = delegate.contentType()
        override fun contentLength(): Long = delegate.contentLength()

        override fun writeTo(sink: okio.BufferedSink) {
            val totalBytes = contentLength()
            if (totalBytes <= 0L || onProgress == null) {
                delegate.writeTo(sink)
                return
            }

            val countingSink = object : okio.ForwardingSink(sink) {
                var bytesWritten = 0L
                override fun write(source: okio.Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    bytesWritten += byteCount
                    val percent = ((bytesWritten * 100) / totalBytes).toInt().coerceIn(0, 100)
                    onProgress.invoke(percent)
                }
            }
            val bufferedSink = countingSink.buffer()
            delegate.writeTo(bufferedSink)
            bufferedSink.flush()
        }
    }
}
