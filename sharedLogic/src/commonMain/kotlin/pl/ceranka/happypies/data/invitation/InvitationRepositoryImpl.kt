package pl.ceranka.happypies.data.invitation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.firebase.firestore.CollectionQuery
import pl.ceranka.happypies.firebase.firestore.DocumentReader
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.documentIdOrThrow
import pl.ceranka.happypies.firebase.firestore.documentsOrThrow
import pl.ceranka.happypies.firebase.functions.FunctionCallResult
import pl.ceranka.happypies.firebase.functions.FunctionsGateway

private typealias Fields = InvitationFirestoreFields
private typealias Functions = InvitationFunctions

class InvitationRepositoryImpl(
    private val firestore: FirestoreGateway,
    private val functions: FunctionsGateway
) : InvitationRepository {

    private val changed = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val changes: Flow<Unit> = changed

    override suspend fun getInvitations(trainerId: String): List<Invitation> {
        val query = CollectionQuery(
            collection = Fields.COLLECTION,
            whereEqualTo = mapOf(Fields.TRAINER_ID to StringValue(trainerId))
        )
        return firestore.query(query).documentsOrThrow().map { it.toInvitation() }
    }

    override suspend fun invite(name: String, email: String) {
        callFunction(Functions.INVITE_CLIENT, mapOf(Functions.NAME to name, Functions.EMAIL to email))
        changed.tryEmit(Unit)
    }

    override suspend fun resend(invitationId: String) {
        callFunction(Functions.RESEND_INVITATION, mapOf(Functions.INVITATION_ID to invitationId))
        changed.tryEmit(Unit)
    }

    override suspend fun cancel(invitationId: String) {
        firestore.delete(Fields.COLLECTION, invitationId).documentIdOrThrow()
        changed.tryEmit(Unit)
    }

    override suspend fun getInvitation(code: String): InvitationPreview {
        val data = callFunction(Functions.GET_INVITATION, mapOf(Functions.CODE to code))
        return InvitationPreview(
            name = data.requireValue(Functions.NAME),
            email = data.requireValue(Functions.EMAIL),
            trainerName = data.requireValue(Functions.TRAINER_NAME),
            expiresAt = data.requireValue(Functions.EXPIRES_AT_MILLIS).toLongOrNull()
                ?: throw DataException.MalformedData("${Functions.GET_INVITATION} returned an invalid ${Functions.EXPIRES_AT_MILLIS}")
        )
    }

    override suspend fun accept(code: String, password: String) {
        callFunction(Functions.ACCEPT_INVITATION, mapOf(Functions.CODE to code, Functions.PASSWORD to password))
    }

    private suspend fun callFunction(name: String, data: Map<String, String>): Map<String, String> =
        when (val result = functions.call(name, data)) {
            is FunctionCallResult.Success -> result.data
            is FunctionCallResult.Rejected -> throw result.reason.toException(name)
            is FunctionCallResult.Failure -> throw result.error
        }
}

private fun String.toException(functionName: String): Exception = when (this) {
    Functions.REASON_ACCOUNT_EXISTS -> InvitationException.AccountExists()
    Functions.REASON_INVITATION_EXISTS -> InvitationException.AlreadyInvited()
    Functions.REASON_EXPIRED -> InvitationException.Expired()
    else -> DataException.Unknown("$functionName rejected with an unknown reason '$this'")
}

private fun Map<String, String>.requireValue(key: String): String =
    get(key) ?: throw DataException.MalformedData("Function result is missing '$key'")

private fun FirestoreDocument.toInvitation(): Invitation {
    val reader = DocumentReader(Fields.COLLECTION, this)
    return Invitation(
        id = id,
        name = reader.requireString(Fields.NAME),
        email = reader.requireString(Fields.EMAIL),
        expiresAt = reader.requireMillis(Fields.EXPIRES_AT)
    )
}