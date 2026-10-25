package pl.ceranka.happypies.di

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthStateListener
import pl.ceranka.happypies.auth.AuthUser
import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DocumentListener
import pl.ceranka.happypies.data.firestore.DocumentResult
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.QueryListener
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.firestore.WriteResult
import pl.ceranka.happypies.screen.client_details.ClientDetailsViewModel
import pl.ceranka.happypies.screen.client_list.ClientListViewModel
import pl.ceranka.happypies.screen.clienthome.ClientHomeViewModel
import pl.ceranka.happypies.screen.login.LoginViewModel
import pl.ceranka.happypies.screen.report_details.ReportDetailsViewModel
import pl.ceranka.happypies.screen.report_form.AddReportViewModel
import pl.ceranka.happypies.screen.report_form.EditReportViewModel
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
    fun resolvesClientHomeViewModelWithClientId() {
        assertNotNull(koin.get<ClientHomeViewModel> { parametersOf("client-1") })
    }

    @Test
    fun resolvesReportDetailsViewModelWithClientAndReportId() {
        assertNotNull(koin.get<ReportDetailsViewModel> { parametersOf("client-1", "r1") })
    }

    @Test
    fun resolvesAddReportViewModelWithClientId() {
        assertNotNull(koin.get<AddReportViewModel> { parametersOf("client-1") })
    }

    @Test
    fun resolvesEditReportViewModelWithReportId() {
        assertNotNull(koin.get<EditReportViewModel> { parametersOf("r1") })
    }
}

private object FakeAuthManager : AuthManager {
    override val authUser: AuthUser? = null
    override suspend fun signIn(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override fun addAuthStateListener(listener: AuthStateListener): ListenerHandle {
        listener.onAuthStateChanged()
        return ListenerHandle {}
    }
}

private object FakeFirestoreGateway : FirestoreGateway {
    override suspend fun getDocument(collection: String, id: String) = DocumentResult.Success(null)
    override suspend fun query(query: CollectionQuery) = ReadResult.Success(emptyList())
    override suspend fun add(collection: String, fields: Map<String, FirestoreValue>) = WriteResult.Success("id")
    override suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>) = WriteResult.Success(id)
    override suspend fun delete(collection: String, id: String) = WriteResult.Success(id)
    override fun addDocumentListener(collection: String, id: String, listener: DocumentListener) = ListenerHandle {}
    override fun addQueryListener(query: CollectionQuery, listener: QueryListener) = ListenerHandle {}
}