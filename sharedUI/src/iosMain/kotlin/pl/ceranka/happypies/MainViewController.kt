package pl.ceranka.happypies

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRepository

fun MainViewController(
    authManager: AuthManager,
    userRepository: UserRepository,
): UIViewController =
    ComposeUIViewController {
        App(
            authManager = authManager,
            userRepository = userRepository,
        )
    }
