package pl.ceranka.happypies.firebase.functions

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.FirebaseFunctionsException.Code
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pl.ceranka.happypies.core.DataException

class AndroidFunctionsGateway(private val functions: FirebaseFunctions) : FunctionsGateway {

    override suspend fun call(
        name: String,
        data: Map<String, String>
    ): FunctionCallResult {
        return try {
            val result = functions.getHttpsCallable(name).call(data).await()
            result.data.toStringMap()
                ?.let { FunctionCallResult.Success(it) }
                ?: FunctionCallResult.Failure(DataException.MalformedData("Function $name returned ${result.data}"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFunctionsException) {
            e.reason()?.let { FunctionCallResult.Rejected(it) } ?: FunctionCallResult.Failure(e.toDataException())
        } catch (e: Exception) {
            FunctionCallResult.Failure(DataException.Unknown(e.message))
        }
    }

}

private fun Any?.toStringMap(): Map<String, String>? {
    if (this !is Map<*, *>) return null
    if (!all { (key, value) -> key is String && value is String }) return null
    @Suppress("UNCHECKED_CAST")
    return this as Map<String, String>
}

private fun FirebaseFunctionsException.reason(): String? = (details as? Map<*, *>)?.get("reason") as? String

private fun FirebaseFunctionsException.toDataException(): DataException = when (code) {
    Code.NOT_FOUND -> DataException.NotFound(message)
    Code.PERMISSION_DENIED, Code.UNAUTHENTICATED -> DataException.PermissionDenied(message)
    Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> DataException.Unavailable(message)
    else -> DataException.Unknown(message)
}