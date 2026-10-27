# Next session: first Android build

## Current known state

- Repository: https://github.com/spradlev/scholarly-feed
- The user reports that the GitHub repository is fresh and empty.
- Android Studio is installed and running on a Zorin OS Linux development PC.
- It is not yet confirmed whether a project has been created, Gradle has synced, or an app has run.
- The user wants step-by-step guidance and to learn while building.

## Goal

Get the smallest Kotlin + Jetpack Compose app to build and launch, then make the first Git commit. Do not add RSS, Room, the proxy, or AI before the baseline works.

## Steps

1. In Android Studio, inspect **Tools → SDK Manager** (or SDK Manager from the welcome screen).
2. Confirm an Android SDK platform and Android SDK Platform-Tools are installed.
3. In **Tools → Device Manager**, confirm an emulator exists or choose a physical Android device.
4. If no project exists, create a new **Empty Activity** project using Kotlin and Jetpack Compose.
5. Suggested project name: `Scholarly Feed`.
6. Suggested package: `dev.spradlev.scholarlyfeed` (confirm before using if the owner prefers a different namespace).
7. Suggested local path: `~/AndroidStudioProjects/scholarly-feed`.
8. Use API 26 as a provisional minimum SDK only if available and appropriate; revisit before release.
9. Wait for Gradle sync; run the starter app.
10. Capture any exact error text rather than guessing at a fix.
11. Initialize Git, inspect `.gitignore`, and commit the verified baseline.
12. Add this documentation package and push to the GitHub repository, resolving any remote initialization conflict carefully.

## Important

- Do not say the build works until it has been run successfully.
- Do not create a second nested `scholarly-feed/scholarly-feed` project by accident; inspect the selected directory.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- If a command is needed, explain what it does before asking a beginner to run it.
