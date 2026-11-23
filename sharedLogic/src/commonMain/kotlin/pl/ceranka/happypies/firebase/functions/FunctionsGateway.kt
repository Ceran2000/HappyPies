package pl.ceranka.happypies.firebase.functions

import pl.ceranka.happypies.core.DataException

interface FunctionsGateway {

    @Throws(Exception::class)
    suspend fun call(name: String, data: Map<String, String>): FunctionCallResult
}

sealed class FunctionCallResult {
    data class Success(val data: Map<String, String>) : FunctionCallResult()

    // The function refused on purpose: HttpsError with details.reason.
    data class Rejected(val reason: String) : FunctionCallResult()

    data class Failure(val error: DataException) : FunctionCallResult()
}