import FirebaseFunctions
import Shared

final class IosFunctionsGateway: FunctionsGateway {
    
    private let functions: Functions
    
    init(functions: Functions) {
        self.functions = functions
    }
    
    func call(name: String, data: [String : String]) async throws -> FunctionCallResult {
        do {
            let result = try await functions.httpsCallable(name).call(data)
            guard let response = result.data as? [String: String] else {
                return FunctionCallResult.Failure(error: DataException.MalformedData(message: "Function \(name) returned \(result.data)"))
            }
            return FunctionCallResult.Success(data: response)
        } catch {
            if error is CancellationError { throw error }
            let nsError = error as NSError
            if let reason = reason(of: nsError) {
                return FunctionCallResult.Rejected(reason: reason)
            }
            return FunctionCallResult.Failure(error: classify(nsError))
        }
    }
}

private func reason(of error: NSError) -> String? {
    guard error.domain == FunctionsErrorDomain else { return nil }
    let details = error.userInfo[FunctionsErrorDetailsKey] as? [String: Any]
    return details?["reason"] as? String
}

private func classify(_ error: NSError) -> DataException {
    let message = error.localizedDescription
    guard error.domain == FunctionsErrorDomain else { return DataException.Unknown(message: message) }
    switch FunctionsErrorCode(rawValue: error.code) {
    case .notFound: return DataException.NotFound(message: message)
    case .permissionDenied, .unauthenticated: return DataException.PermissionDenied(message: message)
    case .unavailable, .deadlineExceeded: return DataException.Unavailable(message: message)
    default: return DataException.Unknown(message: message)
    }
}