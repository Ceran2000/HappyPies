package pl.ceranka.happypies

import android.app.Application
import org.koin.core.context.startKoin
import pl.ceranka.happypies.di.appModule
import pl.ceranka.happypies.di.firebaseModule
import pl.ceranka.happypies.di.viewModelModule

class HappyPiesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(firebaseModule, appModule, viewModelModule)
        }
    }
}
