package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    sealed interface SignedOut : Route

    @Serializable
    sealed interface SignedIn : Route

    @Serializable
    data object Login : SignedOut

    @Serializable
    data object InvitationCode : SignedOut

    @Serializable
    data class AcceptInvitation(val code: String) : SignedOut

    @Serializable
    data object ClientList : SignedIn

    @Serializable
    data object InviteClient : SignedIn

    @Serializable
    data class ClientHome(val clientId: String) : SignedIn

    @Serializable
    data class ClientDetail(val clientId: String) : SignedIn

    @Serializable
    data class ReportDetail(val clientId: String, val reportId: String) : SignedIn

    @Serializable
    data class AddReport(val clientId: String) : SignedIn

    @Serializable
    data class EditReport(val reportId: String) : SignedIn
}