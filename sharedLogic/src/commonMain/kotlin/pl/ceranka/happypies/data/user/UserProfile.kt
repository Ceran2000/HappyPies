package pl.ceranka.happypies.data.user

data class UserProfile(val uid: String, val name: String, val email: String, val role: UserRole)