# Decision log

This file distinguishes user-confirmed decisions from proposals and open questions.

## Confirmed

- **Product:** Scholarly Feed, a distraction-free chronological reader for scholarly/scientific and civic news.
- **Development order:** Native Android first using Kotlin; cross-platform options can be considered later.
- **Initial app:** One native Android application module using Kotlin and Jetpack Compose.
- **Offline strategy:** Cached articles and saved sources should work offline. AI features may require internet.
- **GitHub:** The project is maintained at https://github.com/spradlev/scholarly-feed.
- **Development environment:** Android Studio is installed and running on a Zorin OS Linux development PC.
- **Learning preference:** The product owner expects beginner-friendly Android Studio guidance and wants to learn as the project develops.
- **Source persistence:** Room stores verified feed-source metadata on the device.
- **Feed fetching:** The Android app fetches user-entered HTTPS RSS/Atom URLs directly, with redirect, timeout, response-size, content-type, and destination-address checks. The user chose direct device fetching over a backend for this stage.
- **Feed parser:** `com.prof18.rssparser:rssparser` parses RSS and Atom documents.

## Recommended, not yet implemented

- Room for local article cache and DataStore for preferences.
- ViewModel/repository separation.
- GitHub Actions for automated build/test checks.
- Private repository during early development.
- API-26 minimum Android version as an initial proposal; reconsider after requirements and dependencies are checked.

## Open questions

- Which feed endpoints pass real-world validation? No candidate source URL has been tested against a live publisher feed yet.
- What cache/refresh behavior should be implemented for articles?
- Which LLM provider will be used behind the provider-neutral proxy?
- Where will the proxy be hosted?
- What license should apply before any public release?
- What is the final minimum Android version after accessibility, device, and dependency trade-offs are assessed?

Update this log when a decision is confirmed. Do not turn a proposal into a “confirmed” decision without the product owner's agreement or actual verification.
