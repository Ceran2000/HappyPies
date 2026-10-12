package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.auth.SessionState
import pl.ceranka.happypies.fakes.FakeAuthManager
import pl.ceranka.happypies.fakes.FakeUserRepository
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val auth = FakeAuthManager()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun signIn(session: SessionManager): LoginViewModel = LoginViewModel(session).apply {
        emailState.setTextAndPlaceCursorAtEnd("jan@przyklad.pl")
        passwordState.setTextAndPlaceCursorAtEnd("secret")
        signIn()
    }

    @Test
    fun successfulSignInSignsTheSessionIn() {
        val session = SessionManager(auth, FakeUserRepository(trainer))

        signIn(session)

        assertEquals(SessionState.SignedIn(trainer), session.state.value)
    }

    @Test
    fun missingProfileShowsAMessageAndStaysSignedOut() {
        val session = SessionManager(auth, FakeUserRepository())

        val viewModel = signIn(session)

        assertEquals(LoginUiState.Error("Nie znaleziono danych. Mogły zostać usunięte."), viewModel.uiState.value)
        assertEquals(null, auth.current)
    }

    @Test
    fun authErrorShowsASignInMessageWithoutTechnicalText() {
        auth.signInError = IllegalStateException("The password is invalid or the user does not have a password.")
        val session = SessionManager(auth, FakeUserRepository(trainer))

        val viewModel = signIn(session)

        assertEquals(
            LoginUiState.Error("Nie udało się zalogować. Sprawdź e-mail, hasło i połączenie z internetem."),
            viewModel.uiState.value,
        )
    }
}
