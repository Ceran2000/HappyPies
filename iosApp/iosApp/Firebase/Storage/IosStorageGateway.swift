import FirebaseStorage
import Shared

final class IosStorageGateway: StorageGateway {
    
    private let storage: Storage
    
    init(storage: Storage) {
        self.storage = storage
    }
    
    func startUpload(localFileUri: String, storagePath: String, listener: any UploadListener) -> any UploadHandle {
        let reference = storage.reference(withPath: storagePath)
        guard let url = URL(string: localFileUri) else {
            listener.onUploadEvent(event: UploadEvent.Failure(error: DataException.Unknown(message: "Invalid file URI: \(localFileUri)")))
            return StorageUploadHandle {}
        }
        let task = reference.putFile(from: url)
        task.observe(.progress) { snapshot in
            guard let progress = snapshot.progress else { return }
            listener.onUploadEvent(event: UploadEvent.Progress(uploadedBytes: progress.completedUnitCount, totalBytes: progress.totalUnitCount))
        }
        task.observe(.success) { snapshot in
            listener.onUploadEvent(event: UploadEvent.Success.shared)
        }
        task.observe(.failure) { snapshot in
            listener.onUploadEvent(event: UploadEvent.Failure(error: classify(snapshot.error)))
        }
        return StorageUploadHandle { task.cancel() }
    }
    
    func delete(storagePath: String) async throws -> DeleteResult {
        do {
            try await storage.reference(withPath: storagePath).delete()
            return DeleteResult.Success.shared
        } catch {
            if error is CancellationError { throw error }
            return DeleteResult.Failure(error: classify(error))
        }
    }
    
    func getDownloadUrl(storagePath: String) async throws -> DownloadUrlResult {
        do {
            let downloadUrl = try await storage.reference(withPath: storagePath).downloadURL().absoluteString
            return DownloadUrlResult.Success(url: downloadUrl)
        } catch {
            if error is CancellationError { throw error }
            return DownloadUrlResult.Failure(error: classify(error))
        }
    }

}

private final class StorageUploadHandle: UploadHandle {
    
    private let onCancel: () -> Void
    
    init(onCancel: @escaping () -> Void) {
        self.onCancel = onCancel
    }
    
    func cancel() {
        onCancel()
    }
}

private func classify(_ error: Error?) -> DataException {
    guard let error else { return DataException.Unknown(message: nil) }
    let nsError = error as NSError
    let message = nsError.localizedDescription
    switch (nsError.domain, StorageErrorCode(rawValue: nsError.code)) {
    case (StorageErrorDomain, .objectNotFound): return DataException.NotFound(message: message)
    case (StorageErrorDomain, .unauthorized), (StorageErrorDomain, .unauthenticated): return DataException.PermissionDenied(message: message)
    case (StorageErrorDomain, .retryLimitExceeded): return DataException.Unavailable(message: message)
    default: return DataException.Unknown(message: message)
    }
}