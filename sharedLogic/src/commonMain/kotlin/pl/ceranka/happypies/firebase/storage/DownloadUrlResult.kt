package pl.ceranka.happypies.firebase.storage

import pl.ceranka.happypies.core.DataException

sealed class DownloadUrlResult {
    data class Success(val url: String) : DownloadUrlResult()
    data class Failure(val error: DataException) : DownloadUrlResult()
}