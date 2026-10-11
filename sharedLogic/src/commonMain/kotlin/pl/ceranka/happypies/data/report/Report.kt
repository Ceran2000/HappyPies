package pl.ceranka.happypies.data.report

import pl.ceranka.happypies.data.user.UserRole

enum class MediaType { PHOTO, VIDEO }

data class MediaItem(val type: MediaType, val storagePath: String, val sizeBytes: Long)

data class ReportComment(
    val authorId: String,
    val authorName: String,
    val authorRole: UserRole,
    val text: String,
    val updatedAt: Long,
)

data class Report(
    val id: String,
    val clientId: String,
    val title: String,
    val description: String?,
    val authorId: String,
    val authorName: String,
    val authorRole: UserRole,
    val lastEditedBy: String,
    val lastEditedByName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val comment: ReportComment?,
    val media: List<MediaItem>,
)

data class NewReport(
    val clientId: String,
    val title: String,
    val description: String?,
    val authorId: String,
    val authorName: String,
    val authorRole: UserRole,
)