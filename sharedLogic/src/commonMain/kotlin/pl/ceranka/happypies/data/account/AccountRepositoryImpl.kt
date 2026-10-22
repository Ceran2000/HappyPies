package pl.ceranka.happypies.data.account

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthUser
import pl.ceranka.happypies.auth.authUserFlow
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
    private val scope: CoroutineScope,
) : AccountRepository {

    private val log = Logger.withTag("Account")

    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    override val state: StateFlow<AccountState> = _state.asStateFlow()

    init {
        scope.launch {
            authManager.authUserFlow.collectLatest { user ->
                if (user == null) {
                    // Keeps SignedOut(rejected): the sign-out after a rejection also ends up here.
                    if (state.value !is AccountState.SignedOut) _state.value = AccountState.SignedOut()
                } else {
                    load(user)
                }
            }
        }
    }

    private suspend fun load(user: AuthUser) {
        _state.value = AccountState.Loading
        val loaded = try {
            AccountState.SignedIn(fetchAccount(user))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Loading the account of ${user.uid} failed" }
            if (e.unusable) AccountState.SignedOut(rejected = true) else AccountState.Unavailable(e)
        }

        // A retry can finish after the user changed (e.g. signed out meanwhile); its result is stale then.
        if (user != authManager.authUser) return
        _state.value = loaded
        if (loaded is AccountState.SignedOut) signOutOfFirebase()
    }

    private suspend fun fetchAccount(user: AuthUser): Account {
        val document = gateway.getDocument(Fields.COLLECTION, user.uid)
            .documentOrThrow { "User ${user.uid} does not exist" }
        val reader = DocumentReader(Fields.COLLECTION, document)
        return Account(
            uid = user.uid,
            email = user.email,
            name = reader.requireString(Fields.NAME),
            role = reader.requireEnum(Fields.ROLE, UserRole.entries),
        )
    }

    override fun retry() {
        scope.launch {
            val user = authManager.authUser ?: return@launch     //TODO: handle null
            load(user)
        }
    }

    private suspend fun signOutOfFirebase() {
        try {
            authManager.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Firebase sign-out failed" }
        }
    }
}

private val Exception.unusable: Boolean get() = this is DataException.NotFound || this is DataException.MalformedData || this is DataException.PermissionDenied