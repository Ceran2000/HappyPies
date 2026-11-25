package pl.ceranka.happypies

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.fakes.FakeAccountRepository
import pl.ceranka.happypies.fakes.FakeAuthManager
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.infrastructure.app_links.AppLinks
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val appLinks = AppLinks()
    private val authManager = FakeAuthManager()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(account: FakeAccountRepository) = AppViewModel(account, authManager, appLinks)

    @Test
    fun retriesLoadingTheAccount() {
        val account = FakeAccountRepository(AccountState.Loading)

        viewModel(account).onRetryClick()

        assertEquals(1, account.retries)
    }

    @Test
    fun signsOut() {
        viewModel(FakeAccountRepository(AccountState.SignedIn(trainer))).onLogoutClick()

        assertEquals(1, authManager.signOuts)
    }

    @Test
    fun keepsThePendingInvitationCodeForASignedOutUser() {
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut()))

        appLinks.open(LINK_PATH)

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun keepsThePendingInvitationCodeWhileTheAccountIsLoading() {
        val viewModel = viewModel(FakeAccountRepository(AccountState.Loading))

        appLinks.open(LINK_PATH)

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun dropsThePendingInvitationCodeWhenTheAccountTurnsOutSignedIn() {
        val account = FakeAccountRepository(AccountState.Loading)
        val viewModel = viewModel(account)
        appLinks.open(LINK_PATH)

        account.state.value = AccountState.SignedIn(trainer)

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun ignoresALinkOpenedWhileSignedIn() {
        val account = FakeAccountRepository(AccountState.SignedIn(trainer))
        val viewModel = viewModel(account)
        appLinks.open(LINK_PATH)

        account.state.value = AccountState.SignedOut()

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun ignoresAPathWithoutACode() {
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut()))

        appLinks.open("/i/ABC")

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun keepsALinkOpenedBeforeTheViewModelExists() {
        appLinks.open(LINK_PATH)

        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut()))

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun forgetsThePendingInvitationCodeOnceOpened() {
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut()))
        appLinks.open(LINK_PATH)

        viewModel.onInvitationOpened()

        assertNull(viewModel.pendingInvitationCode.value)
    }

    private companion object {
        const val CODE = "ABCD2345"
        const val LINK_PATH = "/i/$CODE"
    }
}