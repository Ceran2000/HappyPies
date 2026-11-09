package pl.ceranka.happypies.ui.screen.client_list

import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
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
        val viewModel = ClientListViewModel(clients, accountOf(trainer))

        assertEquals(ClientListUiState.Success(listOf(anna), trainer.name), viewModel.uiState.value)
        assertEquals(listOf(trainer.uid), clients.requestedTrainers)
    }

    @Test
    fun showsTheErrorMessage() {
        clients.error = DataException.Unavailable("offline")

        val viewModel = ClientListViewModel(clients, accountOf(trainer))

        assertEquals(
            ClientListUiState.Error(Res.string.error_unavailable),
            viewModel.uiState.value,
        )
    }

    @Test
    fun waitsWhileNobodyIsSignedIn() {
        val viewModel = ClientListViewModel(clients, accountOf(null))

        assertEquals(ClientListUiState.Loading, viewModel.uiState.value)
        assertEquals(emptyList(), clients.requestedTrainers)
    }
}