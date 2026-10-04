package pl.ceranka.happypies.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import pl.ceranka.happypies.auth.AppUser
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthSessionStore
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.screen.login.LoginViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelModuleTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val platformModule = module {
        single<AuthManager> { FakeAuthManager }
        single<UserRepository> { FakeUserRepository }
        single<ClientRepository> { FakeClientRepository }
        single { AuthSessionStore() }
    }

    private val koin = koinApplication { modules(platformModule, viewModelModule) }.koin

    @Test
    fun resolvesLoginViewModel() {
        assertNotNull(koin.get<LoginViewModel>())
    }

    @Test
    fun resolvesClientListViewModel() {
        assertNotNull(koin.get<ClientListViewModel>())
    }

    @Test
    fun resolvesClientDetailsViewModelWithClientId() {
        assertNotNull(koin.get<ClientDetailsViewModel> { parametersOf("client-1") })
    }
}

private object FakeAuthManager : AuthManager {
    override suspend fun signIn(email: String, password: String) = AppUser("uid", email)
    override suspend fun signOut() = Unit
}

private object FakeUserRepository : UserRepository {
    override suspend fun getRole(uid: String) = UserRole.TRAINER
}

private object FakeClientRepository : ClientRepository {
    override suspend fun getClients(trainerId: String) = emptyList<Client>()
    override suspend fun getClient(clientId: String) = Client(clientId, "Test", "test@example.com", emptyList())
}
