package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val routeSavedStateConfiguration: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.Login::class, Route.Login.serializer())
            subclass(Route.ClientsList::class, Route.ClientsList.serializer())
            subclass(Route.ClientHome::class, Route.ClientHome.serializer())
        }
    }
}
