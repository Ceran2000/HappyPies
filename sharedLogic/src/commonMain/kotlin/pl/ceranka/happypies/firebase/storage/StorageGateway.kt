package pl.ceranka.happypies.firebase.storage

interface StorageGateway {

    fun startUpload(localFileUri: String, storagePath: String, listener: UploadListener): UploadHandle

    @Throws(Exception::class)
    suspend fun delete(storagePath: String): DeleteResult
}