package pl.ceranka.happypies

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform