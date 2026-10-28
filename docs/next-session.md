# Next session: run the Android baseline

## Current state

- Repository: https://github.com/spradlev/scholarly-feed
- The repository now contains a minimal Kotlin + Jetpack Compose Android app.
- The debug APK builds successfully with Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00.
- The app has an empty-feed message and an accessible Focus Mode toggle.
- The app was installed and launched on the Medium Phone API 37 emulator.
- The Focus Mode state was verified to switch on and off in the emulator's accessibility hierarchy.
- TalkBack has not yet been tested.
- The user wants step-by-step guidance and to learn while building.

## Goal

Verify the screen and Focus Mode toggle with TalkBack, then review and commit the baseline. Do not add RSS, Room, the proxy, or AI before the baseline works.

## Steps

1. Open this repository root in Android Studio; do not create a nested project.
2. Let Gradle sync finish, using Android Studio's bundled JDK if needed.
3. If needed, start an emulator or connect a physical Android device and run the `app` configuration.
4. Enable TalkBack and confirm the switch announces its label and state.
5. Review the changed files and commit the baseline.

## Important

- Do not say the build works until it has been run successfully.
- Do not create a second nested `scholarly-feed/scholarly-feed` project by accident; inspect the selected directory.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- The documented minimum SDK is currently API 26; revisit it before release.
- If a command is needed, explain what it does before asking a beginner to run it.
