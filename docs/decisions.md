# Decision log

This file distinguishes user-confirmed decisions from proposals and open questions.

## Confirmed

- **Product:** Scholarly Feed, a distraction-free chronological reader for scholarly/scientific and civic news.
- **Development order:** Native Android first using Kotlin; cross-platform options can be considered later.
- **Offline strategy:** Cached articles and saved sources should work offline. AI features may require internet.
- **Existing implementation:** No existing web skeleton files are known to exist; the web app was a concept, not an implemented prototype.
- **GitHub:** Repository created at https://github.com/spradlev/scholarly-feed. At the latest reported state, it is fresh and empty.
- **Development environment:** Android Studio is installed and running on a Zorin OS Linux development PC.
- **Learning preference:** The product owner expects beginner-friendly Android Studio guidance and wants to learn as the project develops.

## Recommended, not yet confirmed by implementation

- Jetpack Compose for UI.
- One Android app module initially.
- Room for local cache and DataStore for preferences.
- ViewModel/repository separation.
- GitHub Actions for automated build/test checks.
- Private repository during early development.
- API-26 minimum Android version as an initial proposal; reconsider after requirements and dependencies are checked.

## Open questions

- Has the Android project already been created in Android Studio?
- Does Gradle sync and the starter build succeed?
- Is an emulator configured, or will a physical device be used?
- Which Android SDK and Kotlin/Compose versions did Android Studio select?
- Which RSS/Atom parser and HTTP client best fit current compatibility and maintenance needs?
- Which LLM provider will be used behind the provider-neutral proxy?
- Where will the proxy be hosted?
- Which starter feed endpoints pass real validation?
- What license should apply before any public release?
- What is the final minimum Android version after accessibility, device, and dependency trade-offs are assessed?

Update this log when a decision is confirmed. Do not turn a proposal into a “confirmed” decision without the product owner's agreement or actual verification.
