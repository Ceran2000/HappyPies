package pl.ceranka.happypies.data.invitation

object InvitationCode {
    const val LENGTH = 8

    private const val LINK_PATH = "/i/"

    fun normalize(input: CharSequence): String = input.toString().trim().uppercase()

    fun fromLinkPath(path: String): String? {
        if (!path.startsWith(LINK_PATH)) return null
        val code = normalize(path.removePrefix(LINK_PATH).trimEnd('/'))
        return if (code.length == LENGTH) code else null
    }
}