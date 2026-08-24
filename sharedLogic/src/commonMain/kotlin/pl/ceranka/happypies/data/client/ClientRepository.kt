package pl.ceranka.happypies.data.client

interface ClientRepository {

    @Throws(Exception::class)
    suspend fun getClients(trainerId: String): List<Client>

    @Throws(Exception::class)
    suspend fun getClient(clientId: String): Client

}