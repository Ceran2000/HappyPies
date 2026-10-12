package pl.ceranka.happypies.fakes

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import pl.ceranka.happypies.auth.AppUser
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.user.UserProfile
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRole

val anna = Client("client-1", "Anna Kowalska", "anna@przyklad.pl", listOf(Dog("Bala", "border collie")))
val trainer = UserProfile("trainer-1", "Jan Trener", "jan@przyklad.pl", UserRole.TRAINER)

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

class FakeAuthManager(var current: AppUser? = null) : AuthManager {
    var signInError: Exception? = null

    override suspend fun signIn(email: String, password: String): AppUser {
        signInError?.let { throw it }
        return AppUser(trainer.uid, email).also { current = it }
    }

    override suspend fun signOut() {
        current = null
    }

    override val currentUser: AppUser? get() = current
}

class FakeUserRepository(private vararg val profiles: UserProfile) : UserRepository {
    override suspend fun getProfile(uid: String): UserProfile =
        profiles.firstOrNull { it.uid == uid } ?: throw DataException.NotFound("User $uid does not exist")
}

/** A session already restored to [profile] (or signed out when null). The fakes never suspend, so this completes at once. */
@OptIn(ExperimentalCoroutinesApi::class)
fun sessionOf(profile: UserProfile?): SessionManager {
    val auth = FakeAuthManager(current = profile?.let { AppUser(it.uid, it.email) })
    val session = SessionManager(auth, FakeUserRepository(*listOfNotNull(profile).toTypedArray()))
    CoroutineScope(UnconfinedTestDispatcher()).launch { session.restore() }
    return session
}

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
    override val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var reports: List<Report> = emptyList()
    var reportsError: Exception? = null

    val created = mutableListOf<NewReport>()
    var createError: Exception? = null

    /** When set, createReport waits for it, so a test can act while saving is in progress. */
    var createGate: CompletableDeferred<Unit>? = null

    override suspend fun getReports(clientId: String): List<Report> {
        reportsError?.let { throw it }
        return reports
    }

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
    override suspend fun deleteReport(reportId: String) = error("Not used")
}
