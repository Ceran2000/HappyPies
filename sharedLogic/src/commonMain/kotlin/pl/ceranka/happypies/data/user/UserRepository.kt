package pl.ceranka.happypies.data.user

interface UserRepository {

    @Throws(Exception::class)
    suspend fun getRole(uid: String): UserRole
}

enum class UserRole { TRAINER, CLIENT }