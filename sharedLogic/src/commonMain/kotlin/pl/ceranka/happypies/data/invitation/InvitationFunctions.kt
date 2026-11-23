package pl.ceranka.happypies.data.invitation

object InvitationFunctions {
    const val INVITE_CLIENT = "inviteClient"
    const val RESEND_INVITATION = "resendInvitation"
    const val GET_INVITATION = "getInvitation"
    const val ACCEPT_INVITATION = "acceptInvitation"

    const val NAME = "name"
    const val EMAIL = "email"
    const val INVITATION_ID = "invitationId"
    const val CODE = "code"
    const val PASSWORD = "password"
    const val TRAINER_NAME = "trainerName"
    const val EXPIRES_AT_MILLIS = "expiresAtMillis"

    const val REASON_ACCOUNT_EXISTS = "account-exists"
    const val REASON_INVITATION_EXISTS = "invitation-exists"
    const val REASON_EXPIRED = "expired"
}