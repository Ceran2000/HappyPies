package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.fakes.FakeAccountRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val accounts = FakeAccountRepository(AccountState.SignedOut())
    private val rejected = LoginUiState.Error("Konto nie jest skonfigurowane. Skontaktuj się z trenerem.")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun signIn(): LoginViewModel = LoginViewModel(accounts).apply {
        emailState.setTextAndPlaceCursorAtEnd("jan@przyklad.pl")
        passwordState.setTextAndPlaceCursorAtEnd("secret")
        onSignIn()
    }

    @Test
    fun signsInWithTheTypedCredentials() {
        val viewModel = signIn()

        assertEquals(listOf("jan@przyklad.pl" to "secret"), accounts.signIns)
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun rejectedAccountAfterSignInShowsTheRejection() {
        accounts.rejectSignIn = true

        val viewModel = signIn()

        assertEquals(rejected, viewModel.uiState.value)
    }

    @Test
    fun accountRejectedAtStartShowsTheRejection() {
        accounts.state.value = AccountState.SignedOut(rejected = true)

        val viewModel = LoginViewModel(accounts)

        assertEquals(rejected, viewModel.uiState.value)
    }

    @Test
    fun authErrorShowsASignInMessageWithoutTechnicalText() {
        accounts.signInError = IllegalStateException("The password is invalid or the user does not have a password.")

        val viewModel = signIn()

        assertEquals(
            LoginUiState.Error("Nie udało się zalogować. Sprawdź e-mail, hasło i połączenie z internetem."),
            viewModel.uiState.value,
        )
    }
}
