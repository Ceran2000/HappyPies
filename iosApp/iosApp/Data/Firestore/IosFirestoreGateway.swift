import FirebaseFirestore
import SharedUI

final class IosFirestoreGateway: FirestoreGateway {

    private let firestore: Firestore

    init(firestore: Firestore) {
        self.firestore = firestore
    }

    func getDocument(collection: String, id: String) async throws -> DocumentResult {
        do {
            let snapshot = try await firestore.collection(collection).document(id).getDocument()
            return DocumentResult.Success(document: snapshot.exists ? toFirestoreDocument(snapshot) : nil)
        } catch {
            return DocumentResult.Failure(error: try dataException(from: error))
        }
    }

    func query(query: CollectionQuery) async throws -> ReadResult {
        do {
            let snapshot = try await platformQuery(query).getDocuments()
            return ReadResult.Success(documents: snapshot.documents.map(toFirestoreDocument))
        } catch {
            return ReadResult.Failure(error: try dataException(from: error))
        }
    }

    func add(collection: String, fields: [String: FirestoreValue]) async throws -> WriteResult {
        do {
            let reference = try await firestore.collection(collection).addDocument(data: try toPlatform(fields))
            return WriteResult.Success(documentId: reference.documentID)
        } catch {
            return WriteResult.Failure(error: try dataException(from: error))
        }
    }

    func update(collection: String, id: String, fields: [String: FirestoreValue]) async throws -> WriteResult {
        do {
            try await firestore.collection(collection).document(id).updateData(try toPlatform(fields))
            return WriteResult.Success(documentId: id)
        } catch {
            return WriteResult.Failure(error: try dataException(from: error))
        }
    }

    func delete(collection: String, id: String) async throws -> WriteResult {
        do {
            try await firestore.collection(collection).document(id).delete()
            return WriteResult.Success(documentId: id)
        } catch {
            return WriteResult.Failure(error: try dataException(from: error))
        }
    }

    func addDocumentListener(collection: String, id: String, listener: DocumentListener) -> ListenerHandle {
        let registration = firestore.collection(collection).document(id).addSnapshotListener { snapshot, error in
            if let error {
                listener.onDocumentResult(result: DocumentResult.Failure(error: classify(error)))
            } else if let snapshot {
                listener.onDocumentResult(result: DocumentResult.Success(document: snapshot.exists ? toFirestoreDocument(snapshot) : nil))
            }
        }
        return FirestoreListenerHandle(registration: registration)
    }

    func addQueryListener(query: CollectionQuery, listener: QueryListener) -> ListenerHandle {
        let request: Query
        do {
            request = try platformQuery(query)
        } catch {
            listener.onQueryResult(result: ReadResult.Failure(error: classify(error)))
            return FirestoreListenerHandle(registration: nil)
        }
        let registration = request.addSnapshotListener { snapshot, error in
            if let error {
                listener.onQueryResult(result: ReadResult.Failure(error: classify(error)))
            } else if let snapshot {
                listener.onQueryResult(result: ReadResult.Success(documents: snapshot.documents.map(toFirestoreDocument)))
            }
        }
        return FirestoreListenerHandle(registration: registration)
    }

    private func platformQuery(_ query: CollectionQuery) throws -> Query {
        var request: Query = firestore.collection(query.collection)
        for (field, value) in query.whereEqualTo {
            request = request.whereField(field, isEqualTo: try toPlatform(value))
        }
        for order in query.orderBy {
            request = request.order(by: order.field, descending: order.descending)
        }
        if let limit = query.limit {
            request = request.limit(to: limit.intValue)
        }
        return request
    }
}

private final class FirestoreListenerHandle: ListenerHandle {

    private let registration: ListenerRegistration?

    init(registration: ListenerRegistration?) {
        self.registration = registration
    }

    func remove() {
        registration?.remove()
    }
}

// Cancellation is rethrown; every other error is returned as data (see FirestoreGateway in sharedLogic).
private func dataException(from error: Error) throws -> DataException {
    if error is CancellationError { throw error }
    return classify(error)
}

private func classify(_ error: Error) -> DataException {
    let nsError = error as NSError
    let message = nsError.localizedDescription
    guard nsError.domain == FirestoreErrorDomain else {
        return DataException.Unknown(message: message)
    }
    switch FirestoreErrorCode.Code(rawValue: nsError.code) {
    case .notFound: return DataException.NotFound(message: message)
    case .permissionDenied, .unauthenticated: return DataException.PermissionDenied(message: message)
    case .unavailable, .deadlineExceeded: return DataException.Unavailable(message: message)
    default: return DataException.Unknown(message: message)
    }
}

// .estimate: a document just written by this device still has a pending server timestamp.
private func toFirestoreDocument(_ snapshot: DocumentSnapshot) -> FirestoreDocument {
    let data = snapshot.data(with: .estimate) ?? [:]
    return FirestoreDocument(id: snapshot.documentID, fields: data.mapValues(toFirestoreValue))
}

private func toFirestoreValue(_ value: Any) -> FirestoreValue {
    switch value {
    case is NSNull:
        return FirestoreValue.NullValue.shared
    case let string as String:
        return FirestoreValue.StringValue(value: string)
    case let timestamp as Timestamp:
        let millis = Int64(timestamp.seconds) * 1000 + Int64(timestamp.nanoseconds / 1_000_000)
        return FirestoreValue.TimestampValue(epochMillis: millis)
    case let number as NSNumber:
        // NSNumber is not converted to Kotlin types automatically, so the kind is decided here.
        if CFGetTypeID(number) == CFBooleanGetTypeID() {
            return FirestoreValue.BooleanValue(value: number.boolValue)
        }
        if CFNumberIsFloatType(number as CFNumber) {
            return FirestoreValue.DoubleValue(value: number.doubleValue)
        }
        return FirestoreValue.LongValue(value: number.int64Value)
    case let array as [Any]:
        return FirestoreValue.ArrayValue(values: array.map(toFirestoreValue))
    case let map as [String: Any]:
        return FirestoreValue.MapValue(fields: map.mapValues(toFirestoreValue))
    default:
        return FirestoreValue.Unsupported(typeName: String(describing: type(of: value)))
    }
}

private func toPlatform(_ fields: [String: FirestoreValue]) throws -> [String: Any] {
    try fields.mapValues(toPlatform)
}

private func toPlatform(_ value: FirestoreValue) throws -> Any {
    switch value {
    case let v as FirestoreValue.StringValue: return v.value
    case let v as FirestoreValue.LongValue: return v.value
    case let v as FirestoreValue.DoubleValue: return v.value
    case let v as FirestoreValue.BooleanValue: return v.value
    case let v as FirestoreValue.TimestampValue:
        return Timestamp(date: Date(timeIntervalSince1970: Double(v.epochMillis) / 1000))
    case let v as FirestoreValue.ArrayValue: return try v.values.map(toPlatform)
    case let v as FirestoreValue.MapValue: return try toPlatform(v.fields)
    case is FirestoreValue.NullValue: return NSNull()
    case is FirestoreValue.ServerTimestamp: return FieldValue.serverTimestamp()
    case is FirestoreValue.Delete: return FieldValue.delete()
    default: throw GatewayConversionError.unsupportedValue(String(describing: value))
    }
}

enum GatewayConversionError: LocalizedError {
    case unsupportedValue(String)

    var errorDescription: String? {
        switch self {
        case .unsupportedValue(let value): return "Cannot write unsupported Firestore value \(value)"
        }
    }
}