package pl.ceranka.happypies.auth

import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.DataErrorKind
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.GatewayError
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.user.UserFirestoreFields
import pl.ceranka.happypies.data.user.UserProfile
import pl.ceranka.happypies.data.user.UserRepositoryImpl
import pl.ceranka.happypies.data.user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeAuthManager(var current: AppUser? = null) : AuthManager {
    var signInError: Exception? = null
    var signOutCount = 0

    override suspend fun signIn(email: String, password: String): AppUser {
        signInError?.let { throw it }
        return AppUser("uid-1", email).also { current = it }
    }

    override suspend fun signOut() {
        signOutCount++
        current = null
    }

    override val currentUser: AppUser? get() = current
}

class SessionManagerTest {

    private val auth = FakeAuthManager()
    private val gateway = FakeFirestoreGateway()
    private val session = SessionManager(auth, UserRepositoryImpl(gateway))

    private val profile = UserProfile("uid-1", "Jan Trener", "jan@przyklad.pl", UserRole.TRAINER)

    private fun profileExists() = gateway.returnDocuments(
        FirestoreDocument(
            "uid-1",
            mapOf(
                UserFirestoreFields.ROLE to StringValue("TRAINER"),
                UserFirestoreFields.NAME to StringValue("Jan Trener"),
                UserFirestoreFields.EMAIL to StringValue("jan@przyklad.pl"),
            ),
        )
    )

    @Test
    fun startsRestoring() {
        assertEquals(SessionState.Restoring, session.state.value)
    }

    @Test
    fun restoreWithoutARememberedUserIsSignedOut() = runTest {
        session.restore()

        assertEquals(SessionState.SignedOut, session.state.value)
    }

    @Test
    fun restoreWithARememberedUserLoadsTheProfile() = runTest {
        auth.current = AppUser("uid-1", "jan@przyklad.pl")
        profileExists()

        session.restore()

        assertEquals(SessionState.SignedIn(profile), session.state.value)
    }

    @Test
    fun restoreWithoutAProfileSignsOutOfAuth() = runTest {
        auth.current = AppUser("uid-1", "jan@przyklad.pl")

        session.restore()

        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals(1, auth.signOutCount)
    }

    @Test
    fun restoreOfflineKeepsTheAuthSession() = runTest {
        auth.current = AppUser("uid-1", "jan@przyklad.pl")
        gateway.readResult = ReadResult.Failure(GatewayError(DataErrorKind.UNAVAILABLE, "offline"))

        session.restore()

        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals(0, auth.signOutCount)
    }

    @Test
    fun restoreRunsOnlyOnce() = runTest {
        session.restore()
        auth.current = AppUser("uid-1", "jan@przyklad.pl")
        profileExists()

        session.restore()

        assertEquals(SessionState.SignedOut, session.state.value)
    }

    @Test
    fun signInLoadsTheProfile() = runTest {
        session.restore()
        profileExists()

        session.signIn("jan@przyklad.pl", "secret")

        assertEquals(SessionState.SignedIn(profile), session.state.value)
    }

    @Test
    fun signInWithoutAProfileSignsOutAndRethrows() = runTest {
        session.restore()

        assertFailsWith<DataException.NotFound> { session.signIn("jan@przyklad.pl", "secret") }

        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals(1, auth.signOutCount)
        assertEquals(null, auth.current)
    }

    @Test
    fun failedAuthSignInKeepsTheStateSignedOut() = runTest {
        session.restore()
        auth.signInError = IllegalStateException("wrong password")

        assertFailsWith<IllegalStateException> { session.signIn("jan@przyklad.pl", "bad") }

        assertEquals(SessionState.SignedOut, session.state.value)
    }

    @Test
    fun signOutEndsSignedOut() = runTest {
        auth.current = AppUser("uid-1", "jan@przyklad.pl")
        profileExists()
        session.restore()

        session.signOut()

        assertEquals(SessionState.SignedOut, session.state.value)
        assertEquals(null, auth.current)
    }
}