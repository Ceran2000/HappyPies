package pl.ceranka.happypies.ui.common


fun firstNameOf(name: String): String? =
    name.trim().split(Regex("\\s+")).first().takeIf { it.isNotEmpty() }