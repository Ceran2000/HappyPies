package pl.ceranka.happypies.ui.screen.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.login_error_account_not_configured
import happypies.sharedui.generated.resources.login_error_sign_in_failed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.fakes.FakeAccountRepository
import pl.ceranka.happypies.fakes.FakeAuthManager
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val auth = FakeAuthManager()
    private val accounts = FakeAccountRepository(AccountState.SignedOut())
    private val rejected = LoginUiState.Error(Res.string.login_error_account_not_configured)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun signIn(): LoginViewModel = LoginViewModel(auth, accounts).apply {
        emailState.setTextAndPlaceCursorAtEnd("jan@przyklad.pl")
        passwordState.setTextAndPlaceCursorAtEnd("secret")
        onSignIn()
    }

    @Test
    fun signsInWithTheTypedCredentials() {
        val viewModel = signIn()

        assertEquals(listOf("jan@przyklad.pl" to "secret"), auth.signIns)
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun rejectedAccountAfterSignInShowsTheRejection() {
        val viewModel = signIn()

        accounts.state.value = AccountState.SignedOut(rejected = true)

        assertEquals(rejected, viewModel.uiState.value)
    }

    @Test
    fun accountRejectedAtStartShowsTheRejection() {
        accounts.state.value = AccountState.SignedOut(rejected = true)

        val viewModel = LoginViewModel(auth, accounts)

        assertEquals(rejected, viewModel.uiState.value)
    }

    @Test
    fun authErrorShowsASignInMessageWithoutTechnicalText() {
        auth.signInError = IllegalStateException("The password is invalid or the user does not have a password.")

        val viewModel = signIn()

        assertEquals(
            LoginUiState.Error(Res.string.login_error_sign_in_failed),
            viewModel.uiState.value,
        )
    }
}