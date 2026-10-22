package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.user.UserRole

/** The name to show for a person in the client's space; reports store people by id only (ADR 0007). */
internal fun personName(personId: String, role: UserRole, client: Client, account: Account?): String = when {
    personId == client.id -> client.name
    personId == account?.uid -> account.name
    role == UserRole.TRAINER -> "Trener"
    else -> "Klient"
}