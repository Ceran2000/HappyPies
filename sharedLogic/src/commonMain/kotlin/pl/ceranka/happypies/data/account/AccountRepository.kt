package pl.ceranka.happypies.data.account

import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {

    val state: StateFlow<AccountState>

    fun retry()
}