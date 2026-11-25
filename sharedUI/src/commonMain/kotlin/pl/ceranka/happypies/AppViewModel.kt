package pl.ceranka.happypies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.invitation.InvitationCode
import pl.ceranka.happypies.infrastructure.app_links.AppLinks

class AppViewModel(private val accountRepository: AccountRepository, appLinks: AppLinks) : ViewModel() {

    private val _pendingInvitationCode = MutableStateFlow<String?>(null)
    val pendingInvitationCode: StateFlow<String?> = _pendingInvitationCode.asStateFlow()

    fun onInvitationOpened() {
        _pendingInvitationCode.update { null }
    }

    private fun onLinkOpened(linkPath: String) {
        if (accountRepository.state.value is AccountState.SignedIn) return
        val code = InvitationCode.fromLinkPath(linkPath) ?: return
        _pendingInvitationCode.update { code }
    }

    init {
        appLinks.linkPaths
            .onEach(::onLinkOpened)
            .launchIn(viewModelScope)

        accountRepository.state
            .filterIsInstance<AccountState.SignedIn>()
            .onEach { _pendingInvitationCode.update { null } }
            .launchIn(viewModelScope)
    }
}