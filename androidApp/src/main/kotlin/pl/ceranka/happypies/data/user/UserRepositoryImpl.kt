package pl.ceranka.happypies.data.user

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(private val firestore: FirebaseFirestore) : UserRepository {

    override suspend fun getRole(uid: String): UserRole {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val role = snapshot.getString("role")
            ?: throw UserMappingException("Missing or invalid field 'role' for user $uid")
        return try {
            UserRole.valueOf(role)
        } catch (e: IllegalArgumentException) {
            throw UserMappingException("Invalid role '$role' for user $uid")
        }
    }
}

class UserMappingException(message: String) : Exception(message)