package pl.ceranka.happypies.data.user

interface UserRepository {

    @Throws(Exception::class)
    suspend fun getProfile(uid: String): UserProfile
}

enum class UserRole { TRAINER, CLIENT }