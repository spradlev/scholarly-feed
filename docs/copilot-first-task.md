# First coding-agent task: establish the Android baseline

Use this task after opening the repository in Android Studio or an IDE with GitHub Copilot.

## Goal

Create the smallest maintainable, buildable native Android baseline for Scholarly Feed. Do not implement RSS or AI yet.

## Before changing files

1. Inspect the repository tree and Git status.
2. Determine whether an Android project already exists. Do not overwrite or nest another project without explaining the consequence.
3. Read `README.md`, `.github/copilot-instructions.md`, and the docs in `docs/`.
4. Report the detected SDK, Gradle, Kotlin, and Compose versions where discoverable.

## Implementation

If no Android project exists, create or guide creation of a standard Kotlin + Jetpack Compose Android app using the currently supported stable Android Studio template. Avoid manually guessing version numbers for Gradle plugins or dependencies.

The first screen should contain:
- App title: “Scholarly Feed”.
- A short empty-state message explaining that sources will appear after feeds are added.
- A clearly labeled Focus Mode toggle that works and exposes its state, even if the initial feed is empty.
- A simple source-management entry point only if it can be implemented without introducing unnecessary architecture.

Use native Compose components and semantic labels. Keep visual styling minimal. Avoid placeholder buttons that appear functional but do nothing. If a control is not implemented, do not present it as an active control.

## Verification

- Run Gradle sync and build using the available environment if possible.
- Run tests if any exist.
- Report exact commands and actual outcomes. If tools are unavailable, state that verification remains for the user to perform.
- Do not claim an emulator launch unless it actually occurred.

## Deliverables

- Complete project files, not isolated snippets.
- Updated README with exact run/build steps.
- A concise summary of changed files, design choices, and known limitations.
- No API keys, secrets, or speculative feed URLs.

## Done when

A clean checkout has a documented path to build and launch the app, the starter screen is usable with TalkBack, and the repository remains in a coherent state.
