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
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DocumentResult
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.firestore.WriteResult
import pl.ceranka.happypies.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.screen.login.LoginViewModel
import pl.ceranka.happypies.screen.report_form.ReportFormViewModel
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
        single<FirestoreGateway> { FakeFirestoreGateway }
    }

    private val koin = koinApplication { modules(platformModule, dataModule, viewModelModule) }.koin

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

    @Test
    fun resolvesReportFormViewModelWithClientId() {
        assertNotNull(koin.get<ReportFormViewModel> { parametersOf("client-1") })
    }
}

private object FakeAuthManager : AuthManager {
    override suspend fun signIn(email: String, password: String) = AppUser("uid", email)
    override suspend fun signOut() = Unit
    override val currentUser: AppUser? = null
}

private object FakeFirestoreGateway : FirestoreGateway {
    override suspend fun getDocument(collection: String, id: String) = DocumentResult.Success(null)
    override suspend fun query(query: CollectionQuery) = ReadResult.Success(emptyList())
    override suspend fun add(collection: String, fields: Map<String, FirestoreValue>) = WriteResult.Success("id")
    override suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>) = WriteResult.Success(id)
    override suspend fun delete(collection: String, id: String) = WriteResult.Success(id)
}
