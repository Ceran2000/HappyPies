package pl.ceranka.happypies.data.account

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthUser
import pl.ceranka.happypies.auth.authUser
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.documentOrThrow
import pl.ceranka.happypies.data.user.UserFirestoreFields
import pl.ceranka.happypies.data.user.UserRole

private typealias Fields = UserFirestoreFields

class AccountRepositoryImpl(
    private val authManager: AuthManager,
    private val gateway: FirestoreGateway,
    scope: CoroutineScope,
) : AccountRepository {

    private val log = Logger.withTag("Account")

    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    override val state: StateFlow<AccountState> = _state.asStateFlow()

    private val retries = MutableStateFlow(0)

    init {
        scope.launch {
            combine(authManager.authUser, retries) { user, _ -> user }.collectLatest { user ->
                if (user == null) {
                    // Keeps SignedOut(rejected): the sign-out after a rejection also ends up here.
                    if (_state.value !is AccountState.SignedOut) _state.value = AccountState.SignedOut()
                } else {
                    _state.value = AccountState.Loading
                    val loaded = loadAccount(user)
                    _state.value = loaded
                    if (loaded is AccountState.SignedOut) signOutQuietly()
                }
            }
        }
    }

    override fun retry() {
        retries.update { it + 1 }
    }

    override suspend fun signIn(email: String, password: String) {
        authManager.signIn(email, password)
    }

    override suspend fun signOut() {
        signOutQuietly()
    }

    private suspend fun loadAccount(user: AuthUser): AccountState = try {
        AccountState.SignedIn(readAccount(user))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.w(e) { "Loading the account of ${user.uid} failed" }
        val unusable = e is DataException.NotFound || e is DataException.MalformedData || e is DataException.PermissionDenied
        if (unusable) AccountState.SignedOut(rejected = true) else AccountState.Unavailable(e)
    }

    private suspend fun readAccount(user: AuthUser): Account {
        // Only email and password sign-in exists, so Firebase always knows the email.
        val email = user.email ?: throw DataException.MalformedData("Firebase user ${user.uid} has no email")
        val document = gateway.getDocument(Fields.COLLECTION, user.uid).documentOrThrow { "User ${user.uid} does not exist" }
        val reader = DocumentReader(Fields.COLLECTION, document)
        return Account(
            uid = user.uid,
            email = email,
            name = reader.requireString(Fields.NAME),
            role = reader.requireEnum(Fields.ROLE, UserRole.entries),
        )
    }

    private suspend fun signOutQuietly() {
        try {
            authManager.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Firebase sign-out failed" }
        }
    }
}
