package pl.ceranka.happypies.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.core.ListenerHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class ListenerOnlyAuthManager : AuthManager {
    val listeners = mutableListOf<AuthStateListener>()

    override var authUser: AuthUser? = null

    fun emit(user: AuthUser?) {
        authUser = user
        listeners.toList().forEach { it.onAuthStateChanged() }
    }

    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit

    override fun addAuthStateListener(listener: AuthStateListener): ListenerHandle {
        listeners += listener
        return ListenerHandle { listeners -= listener }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthUserFlowTest {

    private val auth = ListenerOnlyAuthManager()
    private val jan = AuthUser("uid-1", "jan@przyklad.pl")

    @Test
    fun emitsEveryChangeOnce() = runTest {
        val seen = mutableListOf<AuthUser?>()
        auth.authUserFlow.onEach { seen += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        auth.emit(null)
        auth.emit(jan)
        auth.emit(jan)
        auth.emit(null)

        assertEquals(listOf(null, jan, null), seen)
    }

    @Test
    fun stoppingTheCollectionRemovesTheListener() = runTest {
        val job = auth.authUserFlow.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
        assertEquals(1, auth.listeners.size)

        job.cancel()

        assertTrue(auth.listeners.isEmpty())
    }
}
