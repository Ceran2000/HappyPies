package pl.ceranka.happypies

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.screen.clienthome.ClientHomeScreen

@Composable
fun App(
    authManager: AuthManager,
    userRepository: UserRepository,
) {
    MaterialTheme {
        val backStack = rememberNavBackStack(routeSavedStateConfiguration, Route.Login)
        val scope = rememberCoroutineScope()

        fun logout() {
            scope.launch {
                authManager.signOut()
                backStack.clear()
                backStack.add(Route.Login)
            }
        }

        NavDisplay(
            backStack = backStack,
            entryProvider = entryProvider {
                entry<Route.Login> {
                    LoginScreen(
                        authManager = authManager,
                        userRepository = userRepository,
                        onLoggedIn = { role ->
                            backStack.clear()
                            backStack.add(
                                when (role) {
                                    UserRole.TRAINER -> Route.ClientsList
                                    UserRole.CLIENT -> Route.ClientHome
                                }
                            )
                        }
                    )
                }
                entry<Route.ClientsList> {
                    AuthenticatedScreen(onLogout = ::logout) {
                        ClientHomeScreen()
                    }
                }
                entry<Route.ClientHome> {
                    AuthenticatedScreen(onLogout = ::logout) {
                        ClientHomeScreen()
                    }
                }
            }
        )
    }
}

@Composable
private fun LoginScreen(
    authManager: AuthManager,
    userRepository: UserRepository,
    onLoggedIn: (UserRole) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                errorMessage = null
                isLoading = true
                scope.launch {
                    try {
                        val user = authManager.signIn(email, password)
                        val role = userRepository.getRole(user.uid)
                        onLoggedIn(role)
                    } catch (e: Exception) {
                        errorMessage = e.message
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            Text(if (isLoading) "Logowanie..." else "Zaloguj")
        }
    }
}

@Composable
private fun AuthenticatedScreen(
    onLogout: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onLogout) {
                Text("Wyloguj")
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
