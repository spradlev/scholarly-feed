# Next session: keep building Scholarly Feed

## Current state

- Repository: https://github.com/spradlev/scholarly-feed
- The repository contains a Kotlin + Jetpack Compose Android app with an empty-feed screen, Focus Mode, and source management.
- The debug APK builds successfully with Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00.
- Sources can be added with a title, HTTPS URL, and one of four levels; they can be removed while the app process remains alive.
- Sources are not persisted, verified, or fetched.
- The Compose UI tests cover Focus Mode state and source add/remove; Android lint passes.
- The app has been run on the Medium Phone API 37 emulator.
- TalkBack exposes the Focus Mode label/state on focus, but dynamic state-change announcements were not observed.
- The user wants step-by-step guidance and to learn while building.

## Goal

Continue with a small, tested feature built on the source-management foundation. Keep verified feed ingestion, durable persistence, and AI as separate steps.

## Steps

1. Open this repository root in Android Studio; do not create a nested project.
2. Let Gradle sync finish, using Android Studio's bundled JDK if needed.
3. If needed, start an emulator or connect a physical Android device and run the `app` configuration.
4. Run `JAVA_HOME="/opt/android-studio/jbr" ANDROID_HOME="$HOME/Android/Sdk" ./gradlew :app:connectedDebugAndroidTest` for UI tests.
5. Review the changed files and commit the verified work.

## Important

- Do not say the build works until it has been run successfully.
- Do not create a second nested `scholarly-feed/scholarly-feed` project by accident; inspect the selected directory.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- The documented minimum SDK is currently API 26; revisit it before release.
- If a command is needed, explain what it does before asking a beginner to run it.
