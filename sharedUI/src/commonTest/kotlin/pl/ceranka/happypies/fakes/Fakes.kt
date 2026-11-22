package pl.ceranka.happypies.fakes

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.data.media.MediaRepository
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.report.ReportEntry
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.trainer.Trainer
import pl.ceranka.happypies.data.trainer.TrainerRepository
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.infrastructure.media.CompressedVideo
import pl.ceranka.happypies.infrastructure.media.CompressionHandle
import pl.ceranka.happypies.infrastructure.media.VideoCompressionEvent
import pl.ceranka.happypies.infrastructure.media.VideoCompressionListener
import pl.ceranka.happypies.infrastructure.media.VideoCompressionSettings
import pl.ceranka.happypies.infrastructure.media.VideoCompressor
import pl.ceranka.happypies.infrastructure.media.VideoDurationResult
import pl.ceranka.happypies.infrastructure.media.VideoTranscoder
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.firebase.auth.AuthStateListener
import pl.ceranka.happypies.firebase.auth.AuthUser
import pl.ceranka.happypies.infrastructure.media.CompressedPhoto
import pl.ceranka.happypies.infrastructure.media.PhotoCompressor

val anna = Client("client-1", "Anna Kowalska", "anna@przyklad.pl", trainerId = "trainer-1", listOf(Dog("Bala", "border collie")))
val trainer = Account("trainer-1", "jan@przyklad.pl", "Jan Trener", UserRole.TRAINER)
val annasTrainer = Trainer(trainer.uid, trainer.name)

fun report(
    id: String,
    createdAt: Long = 1_000,
    authorId: String = anna.id,
    authorRole: UserRole = UserRole.CLIENT,
    comment: ReportComment? = null,
    media: List<MediaItem> = emptyList(),
) = Report(
    id = id,
    clientId = anna.id,
    title = "Raport $id",
    description = null,
    authorId = authorId,
    authorRole = authorRole,
    lastEditedBy = authorId,
    createdAt = createdAt,
    updatedAt = createdAt,
    comment = comment,
    media = media,
)

val video = MediaItem(MediaType.VIDEO, "clients/client-1/media/v1", "https://example.com/v1", 24_000_000)

class FakeAccountRepository(initial: AccountState) : AccountRepository {
    override val state = MutableStateFlow(initial)
    var retries = 0

    override fun retry() {
        retries++
    }
}

class FakeAuthManager : AuthManager {
    var signInError: Exception? = null
    val signIns = mutableListOf<Pair<String, String>>()

    override val authUser: AuthUser? = null

    override suspend fun signIn(email: String, password: String) {
        signIns += email to password
        signInError?.let { throw it }
    }

    override suspend fun signOut() = Unit

    override fun addAuthStateListener(listener: AuthStateListener) = ListenerHandle {}
}

fun accountOf(account: Account?) =
    FakeAccountRepository(if (account != null) AccountState.SignedIn(account) else AccountState.SignedOut())

class FakeClientRepository(var client: Client = anna) : ClientRepository {
    var error: Exception? = null
    val requestedTrainers = mutableListOf<String>()

    override suspend fun getClients(trainerId: String): List<Client> {
        requestedTrainers += trainerId
        error?.let { throw it }
        return listOf(client)
    }

    override suspend fun getClient(clientId: String): Client {
        error?.let { throw it }
        return client
    }
}

class FakeTrainerRepository(var trainer: Trainer = annasTrainer) : TrainerRepository {
    var error: Exception? = null

    override suspend fun getTrainer(trainerId: String): Trainer {
        error?.let { throw it }
        return trainer
    }
}

class FakeReportRepository : ReportRepository {
    override val cachedReloads = MutableSharedFlow<Unit>()

    /** The client's reports, all in one page. */
    var reports: List<ReportEntry> = emptyList()

    // The live single report, like liveReports; null means it no longer exists.
    private val liveReport = MutableStateFlow<Result<Report?>>(Result.success(null))

    var report: Report?
        get() = liveReport.value.getOrThrow()
        set(value) {
            liveReport.value = Result.success(value)
        }

    fun failReport(error: Exception) {
        liveReport.value = Result.failure(error)
    }

    val deleted = mutableListOf<String>()
    var deleteError: Exception? = null

    /** When set, deleteReport waits for it, so a test can act while deleting is in progress. */
    var deleteGate: CompletableDeferred<Unit>? = null

    val created = mutableListOf<NewReport>()
    var createError: Exception? = null

    /** When set, createReport waits for it, so a test can act while saving is in progress. */
    var createGate: CompletableDeferred<Unit>? = null

    data class SavedComment(val reportId: String, val text: String, val authorId: String, val authorRole: UserRole)

    val savedComments = mutableListOf<SavedComment>()
    val deletedComments = mutableListOf<String>()
    var commentError: Exception? = null

    /** When set, setComment and deleteComment wait for it, so a test can act while the write is in progress. */
    var commentGate: CompletableDeferred<Unit>? = null

    // Without load states asSnapshot never sees the load finish and waits forever.
    override fun reports(clientId: String): Flow<PagingData<ReportEntry>> = flow {
        val loaded = LoadState.NotLoading(endOfPaginationReached = true)
        emit(PagingData.from(reports, sourceLoadStates = LoadStates(refresh = loaded, prepend = loaded, append = loaded)))
    }

    override fun observeReport(reportId: String): Flow<Report?> = liveReport.map { it.getOrThrow() }

    override suspend fun deleteReport(reportId: String) {
        deleted += reportId
        deleteGate?.await()
        deleteError?.let { throw it }
    }

    override suspend fun createReport(report: NewReport): String {
        created += report
        createGate?.await()
        createError?.let { throw it }
        return "new-id"
    }

    /** What getReport returns; getError makes it fail instead. */
    val stored = mutableMapOf<String, Report>()
    var getError: Exception? = null

    data class Update(val reportId: String, val title: String, val description: String?, val media: List<MediaItem>, val editorId: String)

    val updated = mutableListOf<Update>()
    var updateError: Exception? = null

    override suspend fun getReport(reportId: String): Report {
        getError?.let { throw it }
        return stored.getValue(reportId)
    }

    override suspend fun updateReport(reportId: String, title: String, description: String?, media: List<MediaItem>, editorId: String) {
        updated += Update(reportId, title, description, media, editorId)
        updateError?.let { throw it }
    }

    override suspend fun setComment(reportId: String, text: String, authorId: String, authorRole: UserRole) {
        savedComments += SavedComment(reportId, text, authorId, authorRole)
        commentGate?.await()
        commentError?.let { throw it }
    }

    override suspend fun deleteComment(reportId: String) {
        deletedComments += reportId
        commentGate?.await()
        commentError?.let { throw it }
    }
}

class FakeMediaRepository : MediaRepository {

    data class Upload(val clientId: String, val type: MediaType, val localFileUri: String, val sizeBytes: Long)

    val uploads = mutableListOf<Upload>()

    /** Uploads of these local files fail. */
    val failingUris = mutableSetOf<String>()

    /** When set, each upload reports half of the file and waits for it, so a test can see the progress. */
    var uploadGate: CompletableDeferred<Unit>? = null

    override suspend fun upload(clientId: String, type: MediaType, localFileUri: String, sizeBytes: Long, onProgress: (uploadedBytes: Long) -> Unit): MediaItem {
        uploads += Upload(clientId, type, localFileUri, sizeBytes)
        if (localFileUri in failingUris) throw DataException.Unavailable("offline")
        onProgress(sizeBytes / 2)
        uploadGate?.await()
        onProgress(sizeBytes)
        return uploadedMedia(type, localFileUri, sizeBytes)
    }
}

fun uploadedMedia(type: MediaType, localFileUri: String, sizeBytes: Long) =
    MediaItem(type, "clients/uploaded/media/$localFileUri", "https://example.com/uploaded/$localFileUri", sizeBytes)

fun uploadedVideo(localFileUri: String, sizeBytes: Long) = uploadedMedia(MediaType.VIDEO, localFileUri, sizeBytes)

/** Compressed photos get the URI of the original plus ".jpg" and a tenth of its size. */
class FakePhotoCompressor : PhotoCompressor {
    val compressed = mutableListOf<String>()

    /** Compressing these photos fails. */
    val failingUris = mutableSetOf<String>()

    /** When set, compress waits for it, so a test can act while photos are being prepared. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun compress(uri: String): CompressedPhoto {
        compressed += uri
        gate?.await()
        if (uri in failingUris) throw IllegalStateException("Cannot decode $uri")
        return CompressedPhoto("$uri.jpg", compressedSizes[uri] ?: 300_000)
    }

    val compressedSizes = mutableMapOf<String, Long>()
}

class FakeVideoCompressor : VideoCompressor {
    val compressed = mutableListOf<Pair<String, Long>>()

    /** Compressing these videos fails. */
    val failingUris = mutableSetOf<String>()

    /** Size of a compressed video by source URI; 1_000 bytes when not set. */
    val compressedSizes = mutableMapOf<String, Long>()

    /** When set, compress reports 50% and waits for it, so a test can act mid-compression. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun compress(uri: String, maxSizeBytes: Long, onProgress: (percent: Int) -> Unit): CompressedVideo {
        compressed += uri to maxSizeBytes
        if (uri in failingUris) throw IllegalStateException("Cannot transcode $uri")
        onProgress(50)
        gate?.await()
        onProgress(100)
        return CompressedVideo(uri, compressedSizes[uri] ?: 1_000)
    }
}

class FakeVideoTranscoder : VideoTranscoder {
    var duration: VideoDurationResult = VideoDurationResult.Success(60_000)
    val started = mutableListOf<VideoCompressionSettings>()
    var cancelled = false

    /** Sent to the listener as soon as compression starts; without Success or Failure the compression never ends. */
    var events: List<VideoCompressionEvent> = listOf(VideoCompressionEvent.Success)

    override suspend fun readDuration(sourceUri: String): VideoDurationResult = duration

    override fun startCompression(
        sourceUri: String,
        outputUri: String,
        settings: VideoCompressionSettings,
        listener: VideoCompressionListener
    ): CompressionHandle {
        started += settings
        events.forEach { listener.onCompressionEvent(it) }
        return CompressionHandle { cancelled = true }
    }
}