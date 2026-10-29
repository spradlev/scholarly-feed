# Roadmap and acceptance criteria

Priorities are deliberately sequential. A later phase should not begin until the earlier phase has a reproducible build and a clear test result.

## Phase 0 — Development environment and repository

- [ ] Confirm Android Studio version and installed SDK/platform-tools.
- [x] Create a Kotlin + Jetpack Compose Android project.
- [x] Run the starter app on an emulator or physical Android device.
- [x] Confirm Gradle sync and build succeed.
- [x] Initialize Git, add `.gitignore`, and make the first commit.
- [x] Push to `https://github.com/spradlev/scholarly-feed`.
- [x] Add these project instructions and docs.
- [x] Record SDK, Gradle, Kotlin, and Compose versions actually selected.

**Current baseline:** Android SDK Platform 37, Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00. The debug build, emulator launch, and Focus Mode state changes are verified; TalkBack review remains unverified.

**Done when:** a clean checkout can build using documented instructions, and the starter app launches.

## Phase 1 — Accessible app shell

- [x] Feed screen with a clear empty state.
- [x] Source-management screen with verified add/remove and source levels.
- [x] Focus Mode control and explicit state.
- [x] Apply Focus Mode to show only Level 1 research and scientific reporting.
- [x] Article detail screen using cached feed data, with excerpts distinguished from full text.
- [ ] Native navigation and logical accessibility traversal.
- [ ] TalkBack smoke test and keyboard/focus review where applicable.

**Done when:** the app can be navigated without sight using TalkBack for all implemented flows, with no inert controls or misleading actions.

## Phase 2 — Sources, OPML, and feed engine

- [x] Define four source levels and a source model.
- [x] Search a built-in candidate directory by publisher name or topic; require a manually entered and verified feed URL.
- [x] Hand off a publisher/topic search to the device's web search app.
- [ ] Integrate in-app web search results through a search service.
- [ ] Build a starter OPML with clearly documented candidate feeds.
- [ ] Add, remove, categorize, import, and export sources.
  - [x] Add, remove, and assign a source level.
- [x] Verify user-entered feeds and parse RSS and Atom.
- [x] Sort newest-first, placing undated articles last while preserving their feed order.
- [ ] Deduplicate entries.
- [x] Restrict direct fetches to HTTPS and apply redirect, timeout, response-size, content-type, and address checks.
- [ ] Validate each starter feed URL and parser behavior before release.

**Done when:** tests cover sorting, missing dates, duplicates, OPML import/export, and Focus Mode; starter feed URLs are individually verified.

## Phase 3 — Offline cache

- [x] Add Room persistence for verified source metadata.
- [x] Cache fetched feed entries and excerpts.
- [x] Preserve cached entries when a source refresh fails; replace that source's cache only after a successful fetch.
- [x] Show refresh progress and per-source errors.
- [x] Record and show last successful refresh time per source.
- [ ] Verify reading saved/cached items with network disabled.
- [ ] Design optional cloud sync alongside local-only storage; evaluate Google Drive, Box, Dropbox, and OneDrive before selecting providers.

**Current limitation:** feed entries and available excerpts persist and can be opened in a detail view; full article text is not retrieved or cached.

**Done when:** app restart and offline reading preserve cached sources and articles, and failed refreshes do not erase them.

## Phase 4 — Accessibility and speech

- [ ] Audit labels, roles, traversal order, focus, touch targets, and status announcements.
- [ ] Test with TalkBack on Android.
- [ ] Add optional dictation using supported Android speech services/intents.
- [ ] Add text-to-speech for articles and Librarian answers.
- [ ] Handle unavailable speech services, permissions, cancellation, and errors.
- [ ] Ensure all features remain usable without speech.

**Done when:** speech is optional and graceful failure never blocks core reading.

## Phase 5 — Virtual Librarian

- [ ] Define a provider-neutral request/response contract.
- [ ] Implement backend proxy; never embed provider secrets in the APK.
- [ ] Ground answers in the currently open article text available to the app.
- [ ] Distinguish full text from excerpt/abstract.
- [ ] Separate study findings from press-release claims.
- [ ] Clearly mark when the answer goes beyond the supplied article.
- [ ] Validate suggested feed URLs and parse RSS/Atom before enabling Add.
- [ ] Add rate limiting, request limits, redirect protections, and access/origin restrictions before public deployment.

**Done when:** tests demonstrate that unsupported questions are acknowledged, outside context is labeled, and invalid suggested feeds cannot be added through the suggested-source flow.

## Phase 6 — Hardening and distribution

- [ ] Automated unit tests and Android build in GitHub Actions.
- [ ] Review privacy and data-retention behavior.
- [ ] Test slow/offline/error states and large feeds.
- [ ] Test with a range of screen sizes and Android versions.
- [ ] Document installation and known limitations.
- [ ] Decide on licensing before public release.

## Scope control

Do not implement every phase in one change. Keep the main branch buildable. Prefer a small tested vertical slice over broad scaffolding that has never been run.
