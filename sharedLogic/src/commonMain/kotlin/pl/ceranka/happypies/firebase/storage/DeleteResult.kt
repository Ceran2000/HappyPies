package pl.ceranka.happypies.firebase.storage

import pl.ceranka.happypies.core.DataException

sealed class DeleteResult {
    data object Success : DeleteResult()
    data class Failure(val error: DataException) : DeleteResult()
}