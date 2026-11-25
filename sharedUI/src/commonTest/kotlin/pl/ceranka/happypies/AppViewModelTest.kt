package pl.ceranka.happypies

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.fakes.FakeAccountRepository
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

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun keepsThePendingInvitationCodeForASignedOutUser() {
        val viewModel = AppViewModel(FakeAccountRepository(AccountState.SignedOut()), appLinks)

        appLinks.open(LINK_PATH)

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun keepsThePendingInvitationCodeWhileTheAccountIsLoading() {
        val viewModel = AppViewModel(FakeAccountRepository(AccountState.Loading), appLinks)

        appLinks.open(LINK_PATH)

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun dropsThePendingInvitationCodeWhenTheAccountTurnsOutSignedIn() {
        val account = FakeAccountRepository(AccountState.Loading)
        val viewModel = AppViewModel(account, appLinks)
        appLinks.open(LINK_PATH)

        account.state.value = AccountState.SignedIn(trainer)

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun ignoresALinkOpenedWhileSignedIn() {
        val account = FakeAccountRepository(AccountState.SignedIn(trainer))
        val viewModel = AppViewModel(account, appLinks)
        appLinks.open(LINK_PATH)

        account.state.value = AccountState.SignedOut()

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun ignoresAPathWithoutACode() {
        val viewModel = AppViewModel(FakeAccountRepository(AccountState.SignedOut()), appLinks)

        appLinks.open("/i/ABC")

        assertNull(viewModel.pendingInvitationCode.value)
    }

    @Test
    fun keepsALinkOpenedBeforeTheViewModelExists() {
        appLinks.open(LINK_PATH)

        val viewModel = AppViewModel(FakeAccountRepository(AccountState.SignedOut()), appLinks)

        assertEquals(CODE, viewModel.pendingInvitationCode.value)
    }

    @Test
    fun forgetsThePendingInvitationCodeOnceOpened() {
        val viewModel = AppViewModel(FakeAccountRepository(AccountState.SignedOut()), appLinks)
        appLinks.open(LINK_PATH)

        viewModel.onInvitationOpened()

        assertNull(viewModel.pendingInvitationCode.value)
    }

    private companion object {
        const val CODE = "ABCD2345"
        const val LINK_PATH = "/i/$CODE"
    }
}