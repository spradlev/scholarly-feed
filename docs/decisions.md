# Decision log

This file distinguishes user-confirmed decisions from proposals and open questions.

## Confirmed

- **Product:** Scholarly Feed, a distraction-free chronological reader for scholarly/scientific and civic news.
- **Development order:** Native Android first using Kotlin; cross-platform options can be considered later.
- **Initial app:** One native Android application module using Kotlin and Jetpack Compose.
- **Visual system:** Keep the app native with Jetpack Compose Material 3 components and platform conventions. Within that system, favor restrained angular or squared geometry where it improves the visual identity; do not adopt the Angular web framework or replace native controls with web equivalents.
- **Accessibility target:** Treat WCAG 2.2 Level AA as a design and evaluation benchmark, interpreted for native Android alongside WCAG2ICT and Android accessibility guidance. Do not claim formal conformance without an appropriate evaluation.
- **Offline strategy:** Cached articles and saved sources should work offline. AI features may require internet.
- **Sync provider:** Google Drive is the first optional sync provider. Sync is manual and opt-in; local-only use remains supported.
- **Sync scope and storage:** Sync only the source list as OPML in Drive's private `appDataFolder`, using the `drive.appdata` scope. Cached articles and excerpts remain local.
- **Sync conflicts:** Merge sources by feed URL. Ask before replacing conflicting source title/level metadata; keep local metadata by default. Never propagate deletions. New remote sources must pass the existing feed verification before they are saved locally or the Drive copy is updated.
- **GitHub:** The project is maintained at https://github.com/spradlev/scholarly-feed.
- **Development environment:** Android Studio is installed and running on a Zorin OS Linux development PC.
- **Learning preference:** The product owner expects beginner-friendly Android Studio guidance and wants to learn as the project develops.
- **Source and article persistence:** Room stores verified feed-source metadata and fetched feed-entry metadata/excerpts on the device. Successful fetches replace only that source's cached entries; failed fetches preserve its prior entries.
- **Refresh metadata and bandwidth:** Persist per-source HTTP `ETag` and `Last-Modified` validators and send conditional requests during refresh. A `304 Not Modified` updates the successful-refresh time without replacing cached articles; a changed feed atomically replaces its entries and updates its timestamp and validators. Servers without validator support continue to use normal full-feed refreshes.
- **OPML source levels:** Export standard OPML 2.0 outlines with `text`, `title`, `type`, and `xmlUrl`, plus `scholarlyFeedLevel` to preserve this app's 1-4 levels. Imports without the extension default to Level 1. Imported URLs must still pass HTTPS checks and successful feed verification before saving.
- **Feed fetching:** The Android app fetches user-entered HTTPS RSS/Atom URLs directly, with redirect, timeout, response-size, content-type, and destination-address checks. The user chose direct device fetching over a backend for this stage.
- **Feed parser:** `com.prof18.rssparser:rssparser` parses RSS and Atom documents.
- **Initial Discovery search:** Use Android's external web-search handoff rather than embedding a search provider. Results are not ingested; users manually enter and verify feed URLs.
- **Licensing:** Application source in `app/src/` is licensed under AGPL-3.0-or-later. The starter OPML feed lists and project documentation/design concepts in `docs/` are licensed under CC BY-SA 4.0. Third-party material remains under its own terms.

## Recommended, not yet implemented

- Room for local article cache and DataStore for preferences.
- ViewModel/repository separation.
- GitHub Actions for automated build/test checks.
- Private repository during early development.
- API-26 minimum Android version as an initial proposal; reconsider after requirements and dependencies are checked.

## Open questions

- Which feed endpoints pass real-world validation? No candidate source URL has been tested against a live publisher feed yet.
- Which Google Cloud OAuth project and Android signing certificate will be used to configure and live-test Drive authorization? Do not commit credentials or tokens.
- Which LLM provider will be used behind the provider-neutral proxy?
- Where will the proxy be hosted?
- What is the final minimum Android version after accessibility, device, and dependency trade-offs are assessed?

Update this log when a decision is confirmed. Do not turn a proposal into a “confirmed” decision without the product owner's agreement or actual verification.
