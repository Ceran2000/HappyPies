package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.trainer.Trainer

/** The two people in a client's space: the client and their trainer. Reports store them by id only (ADR 0007). */
internal class ClientSpacePeople(private val client: Client, private val trainer: Trainer) {

    fun nameOf(personId: String): String = if (personId == client.id) client.name else trainer.name
}