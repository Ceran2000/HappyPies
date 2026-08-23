package pl.ceranka.happypies.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import pl.ceranka.happypies.App
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRepository

class MainActivity : ComponentActivity(), KoinComponent {
    private val authManager: AuthManager by inject()
    private val userRepository: UserRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                authManager = authManager,
                userRepository = userRepository,
            )
        }
    }
}