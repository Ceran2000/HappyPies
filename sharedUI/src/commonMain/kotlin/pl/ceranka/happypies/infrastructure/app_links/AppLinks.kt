package pl.ceranka.happypies.infrastructure.app_links

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class AppLinks {

    private val channel = Channel<String>(Channel.CONFLATED)
    val linkPaths: Flow<String> = channel.receiveAsFlow()

    fun open(linkPath: String) {
        channel.trySend(linkPath)
    }
}