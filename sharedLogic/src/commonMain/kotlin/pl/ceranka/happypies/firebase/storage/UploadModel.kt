package pl.ceranka.happypies.firebase.storage

import pl.ceranka.happypies.core.DataException

fun interface UploadListener {
    fun onUploadEvent(event: UploadEvent)
}

sealed class UploadEvent {
    data class Progress(val uploadedBytes: Long, val totalBytes: Long) : UploadEvent()
    data object Success : UploadEvent()
    data class Failure(val error: DataException) : UploadEvent()
}

fun interface UploadHandle {
    fun cancel()
}