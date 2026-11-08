# HappyPies

A mobile app for a dog trainer and their clients, built for Android and iOS from a single Kotlin codebase.

After a training session, either the trainer or the client adds a report describing what they practiced and how it went. The other side reads it and leaves a comment. This keeps the trainer and the client in touch between sessions. The app's UI is in Polish.

## Features

Available now:
- Email sign-in with two roles, trainer and client
- Trainer: a list of clients and each client's details, with their dogs and reports
- Client: a home screen with their own reports and trainer
- Reports with a title and description that can be added, edited and deleted
- One comment per report, written by the side that did not author it
- Paged report lists that keep working offline with cached data
- Light and dark theme

Planned:
- Photos and video in reports
- Client invitations, password reset and a client profile with their dogs

## Tech stack

- **Kotlin Multiplatform**: the logic and data layer are shared between Android and iOS
- **Compose Multiplatform**: the UI and ViewModels are shared, Material 3
- **Firebase** (Auth, Firestore): native Android and iOS SDKs, each implementing a small shared Kotlin interface
- **Koin** for dependency injection, **Navigation 3**, **Paging 3**, **kotlinx-datetime**, **Kermit**

## Modules

- `sharedLogic`: domain models, repositories and the platform-independent Firebase interfaces
- `sharedUI`: screens, ViewModels, theme, navigation and string resources
- `androidApp`: the Android entry point and the Android Firebase implementation
- `iosApp`: the iOS entry point (SwiftUI host) and the iOS Firebase implementation in Swift