package pl.ceranka.happypies.data.report

object ReportFirestoreFields {
    const val COLLECTION = "reports"

    const val CLIENT_ID = "clientId"
    const val TITLE = "title"
    const val DESCRIPTION = "description"
    const val AUTHOR_ID = "authorId"
    const val AUTHOR_ROLE = "authorRole"
    const val LAST_EDITED_BY = "lastEditedBy"
    const val CREATED_AT = "createdAt"
    const val UPDATED_AT = "updatedAt"

    const val COMMENT = "comment"
    const val COMMENT_AUTHOR_ID = "authorId"
    const val COMMENT_AUTHOR_ROLE = "authorRole"
    const val COMMENT_TEXT = "text"
    const val COMMENT_UPDATED_AT = "updatedAt"

    const val MEDIA = "media"
    const val MEDIA_TYPE = "type"
    const val MEDIA_STORAGE_PATH = "storagePath"
    const val MEDIA_SIZE_BYTES = "sizeBytes"
}