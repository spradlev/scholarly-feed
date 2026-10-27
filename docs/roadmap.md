# Roadmap and acceptance criteria

Priorities are deliberately sequential. A later phase should not begin until the earlier phase has a reproducible build and a clear test result.

## Phase 0 — Development environment and repository

- [ ] Confirm Android Studio version and installed SDK/platform-tools.
- [ ] Create a Kotlin + Jetpack Compose Android project.
- [ ] Run the starter app on an emulator or physical Android device.
- [ ] Confirm Gradle sync and build succeed.
- [ ] Initialize Git, add `.gitignore`, and make the first commit.
- [ ] Push to `https://github.com/spradlev/scholarly-feed`.
- [ ] Add these project instructions and docs.
- [ ] Record SDK, Gradle, Kotlin, and Compose versions actually selected.

**Done when:** a clean checkout can build using documented instructions, and the starter app launches.

## Phase 1 — Accessible app shell

- [ ] Feed screen with a clear empty state.
- [ ] Source-management screen.
- [ ] Focus Mode control and explicit state.
- [ ] Article detail screen using plain text.
- [ ] Native navigation and logical accessibility traversal.
- [ ] TalkBack smoke test and keyboard/focus review where applicable.

**Done when:** the app can be navigated without sight using TalkBack for all implemented flows, with no inert controls or misleading actions.

## Phase 2 — Sources, OPML, and feed engine

- [ ] Define four source levels and source data model.
- [ ] Build a starter OPML with clearly documented candidate feeds.
- [ ] Add, remove, categorize, import, and export sources.
- [ ] Parse RSS and Atom feeds.
- [ ] Sort newest-first chronologically, with deterministic handling of missing dates/ties.
- [ ] Deduplicate entries.
- [ ] Apply Focus Mode filtering.
- [ ] Validate each starter feed URL and parser behavior before release.

**Done when:** tests cover sorting, missing dates, duplicates, OPML import/export, and Focus Mode; feed URLs are individually verified.

## Phase 3 — Offline cache

- [ ] Add Room persistence.
- [ ] Cache source metadata and entries/articles.
- [ ] Preserve cached content when refresh fails.
- [ ] Show refresh state and last successful refresh.
- [ ] Verify reading saved/cached items with network disabled.

**Done when:** app restart and offline reading preserve cached sources and articles.

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
