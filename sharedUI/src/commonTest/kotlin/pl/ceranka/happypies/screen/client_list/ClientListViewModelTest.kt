package pl.ceranka.happypies.screen.client_list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.sessionOf
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModelTest {

    private val clients = FakeClientRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTheSignedInTrainersClients() {
        val viewModel = ClientListViewModel(clients, sessionOf(trainer))

        assertEquals(ClientListUiState.Success(listOf(anna)), viewModel.uiState.value)
        assertEquals(listOf(trainer.uid), clients.requestedTrainers)
    }

    @Test
    fun showsTheErrorMessage() {
        clients.error = DataException.Unavailable("offline")

        val viewModel = ClientListViewModel(clients, sessionOf(trainer))

        assertEquals(
            ClientListUiState.Error("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."),
            viewModel.uiState.value,
        )
    }

    @Test
    fun waitsWhileNobodyIsSignedIn() {
        val viewModel = ClientListViewModel(clients, sessionOf(null))

        assertEquals(ClientListUiState.Loading, viewModel.uiState.value)
        assertEquals(emptyList(), clients.requestedTrainers)
    }
}
