import SwiftUI
import SharedUI

struct ComposeView: UIViewControllerRepresentable {
    let authManager: AuthManager
    let userRepository: UserRepository
    let clientRepository: ClientRepository
    let sessionStore: AuthSessionStore

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authManager: authManager,
            userRepository: userRepository,
            clientRepository: clientRepository,
            sessionStore: sessionStore
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}