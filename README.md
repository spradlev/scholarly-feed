# Scholarly Feed

Scholarly Feed is a distraction-free reader for scholarly and scientific articles, with a focus on intentional reading and offline access.

## Project status

The repository contains a minimal native Android app built with Kotlin and Jetpack Compose. Its first screen shows an empty-feed message and a working, accessible Focus Mode toggle. Feed ingestion, persistence, navigation, and AI features are not implemented yet.

## Build and run

Requirements:

- Android Studio with Android SDK Platform 37 installed.
- JDK 17 or newer.
- `ANDROID_HOME` set to the Android SDK installation directory. The commands below use the default Linux path; adjust it if your SDK is elsewhere.
- Internet access for the first Gradle build to download Gradle and dependencies.

On the current Linux setup, Android Studio's bundled JDK is at `/opt/android-studio/jbr`. If Android Studio is installed elsewhere, replace this path with its JBR directory or another JDK 17+.

To build a debug APK from the repository root:

```bash
JAVA_HOME="/opt/android-studio/jbr" ANDROID_HOME="$HOME/Android/Sdk" ./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To install and launch on a connected device or running emulator:

```bash
JAVA_HOME="/opt/android-studio/jbr" ANDROID_HOME="$HOME/Android/Sdk" ./gradlew :app:installDebug
```

Alternatively, open this repository in Android Studio, allow Gradle sync to finish, and run the `app` configuration on a device or emulator.

## Documentation

- [Roadmap and acceptance criteria](docs/roadmap.md)
- [First Android baseline task](docs/copilot-first-task.md)
- [Architecture proposal](docs/architecture.md)
- [Decision log](docs/decisions.md)
- [Accessibility requirements](docs/accessibility.md)
- [Feed source policy](docs/feed-sources.md)
- [Next-session setup notes](docs/next-session.md)
