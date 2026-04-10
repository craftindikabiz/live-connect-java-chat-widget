package com.techindika.liveconnect.model

import java.util.Locale

/**
 * A file attached to a message.
 */
data class Attachment(
    val filename: String,
    val filePath: String,
    val size: Long,
    val type: AttachmentType,
    val mimeType: String
) {
    /** Human-readable file size (e.g. "2.5 MB"). */
    val readableSize: String
        get() {
            val kb = size / 1024.0
            return if (kb < 1024) {
                String.format(Locale.US, "%.1f KB", kb)
            } else {
                String.format(Locale.US, "%.1f MB", kb / 1024.0)
            }
        }

    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType == "application/pdf"

    /** Emoji icon for the file type. */
    val typeEmoji: String
        get() = when {
            isImage -> "\uD83D\uDDBC\uFE0F"
            isPdf -> "\uD83D\uDCC4"
            mimeType.startsWith("video/") -> "\uD83C\uDFAC"
            mimeType.startsWith("audio/") -> "\uD83C\uDFB5"
            else -> "\uD83D\uDCCE"
        }

    /** Validates file size (max 10 MB). Returns error message or null. */
    fun validateSize(): String? {
        val maxBytes = 10L * 1024 * 1024
        return if (size > maxBytes) "File size exceeds 10 MB limit" else null
    }

    companion object {
        /** Max upload size in bytes. */
        const val MAX_FILE_SIZE: Long = 10L * 1024 * 1024

        /** Allowed image extensions. */
        @JvmField
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

        /** Allowed document extensions. */
        @JvmField
        val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "txt", "xls", "xlsx")
    }
}

/** Type of attachment. */
enum class AttachmentType {
    MEDIA,
    DOCUMENT;

    companion object {
        @JvmStatic
        fun fromString(value: String?): AttachmentType = when (value?.lowercase()) {
            "media", "image", "video" -> MEDIA
            "document" -> DOCUMENT
            else -> DOCUMENT
        }
    }
}
