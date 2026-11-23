package pl.ceranka.happypies.firebase.functions

class FakeFunctionsGateway : FunctionsGateway {

    data class Call(val name: String, val data: Map<String, String>)

    var result: FunctionCallResult = FunctionCallResult.Success(emptyMap())
    val calls = mutableListOf<Call>()

    override suspend fun call(name: String, data: Map<String, String>): FunctionCallResult {
        calls += Call(name, data)
        return result
    }
}