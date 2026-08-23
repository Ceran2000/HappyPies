package pl.ceranka.happypies.data.user

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl(private val firestore: FirebaseFirestore) : UserRepository {

    override suspend fun getRole(uid: String): UserRole {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val role = snapshot["role"] as String
        return UserRole.valueOf(role)
    }
}