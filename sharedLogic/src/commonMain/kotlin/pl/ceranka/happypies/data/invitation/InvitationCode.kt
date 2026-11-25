package pl.ceranka.happypies.data.invitation

object InvitationCode {
    const val LENGTH = 8

    fun normalize(input: CharSequence): String = input.toString().trim().uppercase()
}