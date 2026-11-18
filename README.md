# Scholarly Feed

Scholarly Feed is a distraction-light Android app for intentional reading, source curation, and calm discovery. It is built for people who want to consume quality information without the noise, engagement loops, and endless scrolling of a typical social feed.

## Overview

Scholarly Feed helps users stay close to useful, trustworthy, and thoughtfully curated sources. The app emphasizes:

- quiet discovery over viral engagement
- curated feeds over noisy “everything” streams
- source quality over social popularity
- offline reading over constant connectivity
- learning and understanding over reaction-driven consumption

This is not a social network. It is a reading experience designed for attention, depth, and control.

## Current status

The repository contains a native Android app built with Kotlin and Jetpack Compose. The current build is in active on-device testing and refinement, with a focus on:

- validating the core feed collection
- improving offline reading behavior
- reviewing source quality and trust signals
- validating accessibility and reading flow
- refining the learning-oriented feed experience

## Key features

- curated source management with source levels and focus filters
- OPML import/export for source portfolios
- RSS and Atom parsing for flexible source discovery
- article detail screens with excerpts, summaries, and related items
- local search across loaded feed content
- persistent offline caching for sources and entries
- accessibility-minded UI and reading controls

## Why it matters

Many content feeds are optimized for attention rather than understanding. Scholarly Feed takes a different approach:

- fewer distractions
- more deliberate consumption
- stronger source curation
- calmer reading patterns
- a better fit for research, learning, and thoughtful exploration

## Build and run

Requirements:

- Android Studio with Android SDK Platform 37 installed
- JDK 17 or newer
- `ANDROID_HOME` set to the Android SDK installation directory
- internet access for the first Gradle build to download dependencies

On the current Linux setup, Android Studio's bundled JDK is at `/opt/android-studio/jbr`. If Android Studio is installed elsewhere, replace this path with its JBR directory or another JDK 17+.

To build a debug APK from the repository root:

```bash
JAVA_HOME="/opt/android-studio/jbr" ANDROID_HOME="$HOME/Android/Sdk" ./gradlew :app:assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

To run Android instrumentation tests on a connected device or running emulator:

```bash
JAVA_HOME="/opt/android-studio/jbr" ANDROID_HOME="$HOME/Android/Sdk" ./gradlew :app:connectedDebugAndroidTest
```

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

## Product direction

The long-term direction is a calm, learning-oriented feed with thoughtful sources, minimal friction, and a stronger emphasis on reading depth than conventional social feeds.

Near-term focus areas include:

- source-quality curation and trust signaling
- offline-first reading and cache resilience
- mobile-friendly source review workflows
- feed health and freshness monitoring
- deeper reading UX and lower-noise discovery

## Contributing

Contributions are welcome, especially in the following areas:

- feed and source quality review
- accessibility and reading UX
- offline behavior and cache validation
- device testing and usability feedback
- documentation and product refinement

## License

### Application source code

The application source code in `app/src/` is licensed under the GNU Affero General Public License, version 3 or (at your option) any later version. See [LICENSE](LICENSE) for the full terms.

### Curated feed lists and documentation

The starter feed lists in `app/src/main/assets/starter-sources.opml` and `docs/starter-sources.opml`, and project documentation and design concepts in `docs/`, are licensed under the Creative Commons Attribution-ShareAlike 4.0 International License. See [DATA-LICENSE](DATA-LICENSE) for the full terms.

These licenses apply only to material the project has the rights to license. Third-party software, publisher content, trademarks, and other separately identified materials remain under their respective terms.