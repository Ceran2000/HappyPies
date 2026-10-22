package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.user.UserRole

/** The two people in a client's space: the client and their trainer. Reports store them by id only (ADR 0007). */
internal class ClientSpacePeople(private val client: Client, private val trainerName: String) {

    /** The trainer's name is known only when the trainer is signed in; the client sees "Trener" until step 5 loads it. */
    constructor(client: Client, account: Account?) : this(
        client = client,
        trainerName = account?.takeIf { it.role == UserRole.TRAINER }?.name ?: "Trener",
    )

    fun nameOf(personId: String): String = if (personId == client.id) client.name else trainerName
}