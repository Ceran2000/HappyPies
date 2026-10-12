package pl.ceranka.happypies.data.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthUser
import pl.ceranka.happypies.auth.AuthStateListener
import pl.ceranka.happypies.auth.ListenerHandle
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.DataErrorKind
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.GatewayError
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.user.UserFirestoreFields
import pl.ceranka.happypies.data.user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private typealias F = UserFirestoreFields

/** Notifies listeners at once on every change of [authUser], like Firebase Auth. */
private class FakeAuthManager : AuthManager {
    var signOutCount = 0
    private val listeners = mutableListOf<AuthStateListener>()

    override var authUser: AuthUser? = null
        set(value) {
            field = value
            listeners.toList().forEach { it.onAuthStateChanged() }
        }

    override suspend fun signIn(email: String, password: String) {
        authUser = AuthUser("uid-1", email)
    }

    override suspend fun signOut() {
        signOutCount++
        authUser = null
    }

    override fun addAuthStateListener(listener: AuthStateListener): ListenerHandle {
        listeners += listener
        listener.onAuthStateChanged()
        return ListenerHandle { listeners -= listener }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountRepositoryImplTest {

    private val auth = FakeAuthManager()
    private val gateway = FakeFirestoreGateway()
    private val jan = AuthUser("uid-1", "jan@przyklad.pl")
    private val account = Account("uid-1", "jan@przyklad.pl", "Jan Trener", UserRole.TRAINER)

    private val trainerFields: Map<String, FirestoreValue> = mapOf(
        F.ROLE to StringValue("TRAINER"),
        F.NAME to StringValue("Jan Trener"),
        F.EMAIL to StringValue("jan@przyklad.pl"),
    )

    private fun TestScope.repository() =
        AccountRepositoryImpl(auth, gateway, backgroundScope + UnconfinedTestDispatcher(testScheduler))

    private fun userDocument(fields: Map<String, FirestoreValue> = trainerFields) =
        gateway.returnDocuments(FirestoreDocument("uid-1", fields))

    private fun TestScope.rejectedWith(fields: Map<String, FirestoreValue>) {
        auth.authUser = jan
        userDocument(fields)

        assertEquals(AccountState.SignedOut(rejected = true), repository().state.value)
        assertEquals(1, auth.signOutCount)
    }

    @Test
    fun withoutAFirebaseUserIsSignedOut() = runTest {
        assertEquals(AccountState.SignedOut(), repository().state.value)
    }

    @Test
    fun aRememberedUserWithADocumentIsSignedIn() = runTest {
        auth.authUser = jan
        userDocument()

        assertEquals(AccountState.SignedIn(account), repository().state.value)
        assertEquals("users" to "uid-1", gateway.gets.single())
    }

    @Test
    fun emailComesFromFirebaseAuth() = runTest {
        auth.authUser = jan
        userDocument(trainerFields + (F.EMAIL to StringValue("old@przyklad.pl")))

        assertEquals("jan@przyklad.pl", repository().state.value.accountOrNull?.email)
    }

    @Test
    fun missingDocumentIsRejected() = runTest {
        auth.authUser = jan

        assertEquals(AccountState.SignedOut(rejected = true), repository().state.value)
        assertEquals(1, auth.signOutCount)
    }

    @Test
    fun missingNameIsRejected() = runTest {
        rejectedWith(trainerFields - F.NAME)
    }

    @Test
    fun invalidRoleIsRejected() = runTest {
        rejectedWith(trainerFields + (F.ROLE to StringValue("ADMIN")))
    }

    @Test
    fun offlineKeepsTheFirebaseUserAndRetryLoadsTheAccount() = runTest {
        auth.authUser = jan
        gateway.readResult = ReadResult.Failure(GatewayError(DataErrorKind.UNAVAILABLE, "offline"))
        val repository = repository()
        assertIs<DataException.Unavailable>(assertIs<AccountState.Unavailable>(repository.state.value).error)
        assertEquals(0, auth.signOutCount)

        userDocument()
        repository.retry()

        assertEquals(AccountState.SignedIn(account), repository.state.value)
    }

    @Test
    fun retryAfterTheUserSignedOutChangesNothing() = runTest {
        auth.authUser = jan
        gateway.readResult = ReadResult.Failure(GatewayError(DataErrorKind.UNAVAILABLE, "offline"))
        val repository = repository()
        auth.authUser = null

        userDocument()
        repository.retry()

        assertEquals(AccountState.SignedOut(), repository.state.value)
    }

    @Test
    fun signingInMakesTheAccountSignedIn() = runTest {
        val repository = repository()
        userDocument()

        auth.authUser = jan

        assertEquals(AccountState.SignedIn(account), repository.state.value)
    }

    @Test
    fun signingOutEndsSignedOut() = runTest {
        auth.authUser = jan
        userDocument()
        val repository = repository()

        auth.authUser = null

        assertEquals(AccountState.SignedOut(), repository.state.value)
    }

    @Test
    fun signingInAfterARejectionClearsIt() = runTest {
        auth.authUser = jan
        val repository = repository()
        assertEquals(AccountState.SignedOut(rejected = true), repository.state.value)

        userDocument()
        auth.authUser = jan

        assertEquals(AccountState.SignedIn(account), repository.state.value)
    }
}
