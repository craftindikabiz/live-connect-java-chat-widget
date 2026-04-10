package com.techindika.liveconnect.util

import android.webkit.MimeTypeMap

/**
 * File type utilities.
 */
internal object FileUtils {

    /** Get MIME type from file extension. */
    fun getMimeType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    /** Check if a MIME type is an image. */
    fun isImage(mimeType: String): Boolean = mimeType.startsWith("image/")

    /** Check if a MIME type is a document. */
    fun isDocument(mimeType: String): Boolean = mimeType in DOCUMENT_MIME_TYPES

    /** Normalize short file type strings from the server. */
    fun normalizeMimeType(fileType: String): String = when (fileType.lowercase()) {
        "image" -> "image/jpeg"
        "document" -> "application/pdf"
        "video" -> "video/mp4"
        else -> if (fileType.contains("/")) fileType else "application/octet-stream"
    }

    private val DOCUMENT_MIME_TYPES = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
}
