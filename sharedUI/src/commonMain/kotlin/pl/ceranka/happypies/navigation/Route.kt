package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {

    @Serializable
    data object Login : Route

    @Serializable
    data object ClientList : Route

    @Serializable
    data object ClientHome : Route

    @Serializable
    data class ClientDetail(val clientId: String) : Route

    @Serializable
    data class ReportDetail(val clientId: String, val reportId: String) : Route

    @Serializable
    data class AddReport(val clientId: String) : Route

    @Serializable
    data class EditReport(val reportId: String) : Route
}