package pl.ceranka.happypies.data.invitation

import kotlinx.coroutines.flow.Flow

interface InvitationRepository {

    /** Emits after this app invited, re-sent or cancelled an invitation. */
    val changes: Flow<Unit>

    @Throws(Exception::class)
    suspend fun getInvitations(trainerId: String): List<Invitation>

    @Throws(Exception::class)
    suspend fun invite(name: String, email: String)

    @Throws(Exception::class)
    suspend fun resend(invitationId: String)

    @Throws(Exception::class)
    suspend fun cancel(invitationId: String)

    @Throws(Exception::class)
    suspend fun getInvitation(code: String): InvitationPreview

    @Throws(Exception::class)
    suspend fun accept(code: String, password: String): String
}