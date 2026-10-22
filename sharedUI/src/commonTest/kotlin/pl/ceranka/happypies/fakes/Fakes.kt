package pl.ceranka.happypies.fakes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthStateListener
import pl.ceranka.happypies.auth.AuthUser
import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.user.UserRole

val anna = Client("client-1", "Anna Kowalska", "anna@przyklad.pl", listOf(Dog("Bala", "border collie")))
val trainer = Account("trainer-1", "jan@przyklad.pl", "Jan Trener", UserRole.TRAINER)

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

val video = MediaItem(MediaType.VIDEO, "reports/r1/video.mp4", 24_000_000)

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

class FakeReportRepository : ReportRepository {
    // The live reports: a list, or the error that ends every collection until a new list is set.
    private val liveReports = MutableStateFlow<Result<List<Report>>>(Result.success(emptyList()))

    var reports: List<Report>
        get() = liveReports.value.getOrThrow()
        set(value) {
            liveReports.value = Result.success(value)
        }

    fun failReports(error: Exception) {
        liveReports.value = Result.failure(error)
    }

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

    /** How many collections of observeReports / observeReport are running, i.e. open listeners. */
    var activeListeners = 0
        private set

    val deleted = mutableListOf<String>()
    var deleteError: Exception? = null

    /** When set, deleteReport waits for it, so a test can act while deleting is in progress. */
    var deleteGate: CompletableDeferred<Unit>? = null

    val created = mutableListOf<NewReport>()
    var createError: Exception? = null

    /** When set, createReport waits for it, so a test can act while saving is in progress. */
    var createGate: CompletableDeferred<Unit>? = null

    override fun observeReports(clientId: String): Flow<List<Report>> = liveReports.map { it.getOrThrow() }.counted()

    override fun observeReport(reportId: String): Flow<Report?> = liveReport.map { it.getOrThrow() }.counted()

    override suspend fun deleteReport(reportId: String) {
        deleted += reportId
        deleteGate?.await()
        deleteError?.let { throw it }
    }

    private fun <T> Flow<T>.counted() = onStart { activeListeners++ }.onCompletion { activeListeners-- }

    override suspend fun createReport(report: NewReport): String {
        created += report
        createGate?.await()
        createError?.let { throw it }
        return "new-id"
    }

    override suspend fun getReport(reportId: String): Report = error("Not used")
    override suspend fun updateReport(reportId: String, title: String, description: String?, editorId: String) = error("Not used")
    override suspend fun setComment(reportId: String, text: String, authorId: String, authorRole: UserRole) = error("Not used")
    override suspend fun deleteComment(reportId: String) = error("Not used")
}