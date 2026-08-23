import SwiftUI
import SharedUI

struct ComposeView: UIViewControllerRepresentable {
    let authManager: AuthManager
    let userRepository: UserRepository

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            authManager: authManager,
            userRepository: userRepository
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
