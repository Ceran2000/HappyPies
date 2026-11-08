package pl.ceranka.happypies.ui.common

fun greetingFor(name: String): String? =
    name.trim().split(Regex("\\s+")).first().takeIf { it.isNotEmpty() }?.let { "Cześć, $it" }