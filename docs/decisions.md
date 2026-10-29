# Decision log

This file distinguishes user-confirmed decisions from proposals and open questions.

## Confirmed

- **Product:** Scholarly Feed, a distraction-free chronological reader for scholarly/scientific and civic news.
- **Development order:** Native Android first using Kotlin; cross-platform options can be considered later.
- **Initial app:** One native Android application module using Kotlin and Jetpack Compose.
- **Offline strategy:** Cached articles and saved sources should work offline. AI features may require internet.
- **Sync options:** Keep local-only storage available and plan an optional cloud-sync choice for a later stage, with providers such as Google Drive, Box, Dropbox, and OneDrive considered. No provider or sync protocol is selected yet.
- **GitHub:** The project is maintained at https://github.com/spradlev/scholarly-feed.
- **Development environment:** Android Studio is installed and running on a Zorin OS Linux development PC.
- **Learning preference:** The product owner expects beginner-friendly Android Studio guidance and wants to learn as the project develops.
- **Source and article persistence:** Room stores verified feed-source metadata and fetched feed-entry metadata/excerpts on the device. Successful fetches replace only that source's cached entries; failed fetches preserve its prior entries.
- **Refresh timestamps:** Store and display a last-successful-refresh time per source. Initialize it on successful source verification and update it only in the same transaction that replaces cached entries after a successful refresh.
- **OPML source levels:** Export standard OPML 2.0 outlines with `text`, `title`, `type`, and `xmlUrl`, plus `scholarlyFeedLevel` to preserve this app's 1-4 levels. Imports without the extension default to Level 1. Imported URLs must still pass HTTPS checks and successful feed verification before saving.
- **Feed fetching:** The Android app fetches user-entered HTTPS RSS/Atom URLs directly, with redirect, timeout, response-size, content-type, and destination-address checks. The user chose direct device fetching over a backend for this stage.
- **Feed parser:** `com.prof18.rssparser:rssparser` parses RSS and Atom documents.
- **Initial Discovery search:** Use Android's external web-search handoff rather than embedding a search provider. Results are not ingested; users manually enter and verify feed URLs.

## Recommended, not yet implemented

- Room for local article cache and DataStore for preferences.
- ViewModel/repository separation.
- GitHub Actions for automated build/test checks.
- Private repository during early development.
- API-26 minimum Android version as an initial proposal; reconsider after requirements and dependencies are checked.

## Open questions

- Which feed endpoints pass real-world validation? No candidate source URL has been tested against a live publisher feed yet.
- Which cloud providers and sync model should be supported, and how should conflicts and account permissions work?
- Which LLM provider will be used behind the provider-neutral proxy?
- Where will the proxy be hosted?
- What license should apply before any public release?
- What is the final minimum Android version after accessibility, device, and dependency trade-offs are assessed?

Update this log when a decision is confirmed. Do not turn a proposal into a “confirmed” decision without the product owner's agreement or actual verification.
