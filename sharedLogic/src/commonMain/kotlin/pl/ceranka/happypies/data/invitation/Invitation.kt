package pl.ceranka.happypies.data.invitation

data class Invitation(val id: String, val name: String, val email: String, val expiresAt: Long) {
    fun isExpired(nowMillis: Long): Boolean = expiresAt <= nowMillis
}

data class InvitationPreview(val name: String, val email: String, val trainerName: String, val expiresAt: Long)

sealed class InvitationException(reason: String) : Exception(reason) {
    class AccountExists : InvitationException(InvitationFunctions.REASON_ACCOUNT_EXISTS)
    class AlreadyInvited : InvitationException(InvitationFunctions.REASON_INVITATION_EXISTS)
    class Expired : InvitationException(InvitationFunctions.REASON_EXPIRED)
}