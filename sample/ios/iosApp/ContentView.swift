import ComposdownSample
import SwiftUI
import UIKit

/// Hosts the shared Compose Multiplatform sample application.
struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
