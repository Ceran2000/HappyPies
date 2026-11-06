package pl.ceranka.happypies.ui.common

fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).take(2).joinToString("") { it.take(1).uppercase() }