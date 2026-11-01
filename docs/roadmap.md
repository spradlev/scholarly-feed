# Roadmap and acceptance criteria

Priorities are deliberately sequential. A later phase should not begin until the earlier phase has a reproducible build and a clear test result.

## Milestone — usable product

As of 2026-11-01, the app has a usable Android build installed on physical devices and is in a focused period of usability and accessibility testing. The latest milestone adds local search across loaded feed articles and Material tab semantics for Summary and Full article reading modes. The build is installed on the Galaxy S21 for continued hands-on evaluation.

This is a product milestone, not a declaration that development is complete. TalkBack's spoken tab announcements and physical-device behavior still need review. The focused article-mode instrumentation test passes, while the full device suite still has failures to investigate. Drive cross-device validation, external search-service integration, the Virtual Librarian, and release hardening remain on the roadmap.

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
- [x] Keep feed rows concise with clamped titles and summaries, compact source/date metadata, and date-only publication labels; show feed-provided excerpts with attribution in detail.
- [x] When a feed has no usable excerpt, request a concise description from the linked publisher page; clearly distinguish it from feed content and show loading/unavailable/error states. AI-generated fallback is deferred until a backend hosting target is selected.
- [x] Add an on-demand in-app publisher text view beside the summary view; keep article-page text uncached, clearly label extraction limitations, and defer AI-generated summaries until a backend is selected.
- [x] Expand feed previews to five lines; add up to four locally matched related articles below article details; provide a centered Home action alongside Settings and Focus Mode.
- [x] Search loaded feed articles by title, source, or excerpt from the feed screen.
- [ ] Native navigation and logical accessibility traversal.
- [x] Support reverse keyboard focus traversal with Shift+Tab on implemented app screens.
- [ ] TalkBack smoke test and keyboard/focus review where applicable.

**Done when:** the app can be navigated without sight using TalkBack for all implemented flows, with no inert controls or misleading actions.

## Phase 2 — Sources, OPML, and feed engine

- [x] Define four source levels and a source model.
- [x] Search a built-in candidate directory by publisher name or topic; require a manually entered and verified feed URL.
- [x] Hand off a publisher/topic search to the device's web search app.
- [ ] Integrate in-app web search results through a search service.
- [x] Build a starter OPML with clearly documented candidate feeds.
- [x] Add, remove, categorize, import, and export sources.
  - [x] Add, remove, and assign a source level.
  - [x] Import and export verified sources through OPML, preserving source levels.
  - [x] Offer the verified starter OPML directly from source management.
- [x] Verify user-entered feeds and parse RSS and Atom.
- [x] Sort newest-first, placing undated articles last while preserving their feed order.
- [x] Deduplicate entries within a feed by GUID or normalized URL without merging distinct articles or source identities.
- [x] Restrict direct fetches to HTTPS and apply redirect, timeout, response-size, content-type, and address checks.
- [x] Validate each starter feed URL and parser behavior before release.

**Done when:** tests cover sorting, missing dates, duplicates, OPML import/export, and Focus Mode; starter feed URLs are individually verified.

## Phase 3 — Offline cache

- [x] Add Room persistence for verified source metadata.
- [x] Cache fetched feed entries and excerpts.
- [x] Preserve cached entries when a source refresh fails; replace that source's cache only after a successful fetch.
- [x] Show refresh progress and per-source errors.
- [x] Record and show last successful refresh time per source.
- [x] Verify reading saved/cached items from a reopened Room database without network access.
- [x] Select optional, manual Google Drive source-list sync in the private `appDataFolder`; preserve local-only use and keep article cache local.
- [ ] Complete OAuth setup and validate cross-device source sync, including verification failures and conflict choices.

**Current limitation:** feed entries and available excerpts persist. Publisher-page reader text is fetched only on demand and is not cached; extraction can fail or omit content, and AI-generated summaries are not available until a backend is selected.

**Done when:** app restart and offline reading preserve cached sources and articles, and failed refreshes do not erase them.

## Phase 4 — Accessibility and speech

- [x] Add in-app text-size presets and System/Light/Dark color-scheme settings.
- [x] Add a persisted pinch-adjustable reading scale with an accessible settings control; keep content reflowed with vertical scrolling only.
- [ ] Audit labels, roles, traversal order, focus, touch targets, and status announcements.
- [ ] Test with TalkBack on Android.
- [ ] Confirm TalkBack focus through long article text scrolls each focused paragraph into view on a physical phone and tablet.
- [ ] Consider high-contrast and color-vision display options in a later accessibility pass.
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
- [ ] If article images are introduced, show thumbnails and link to the publisher's original article or higher-resolution image.
- [ ] Document installation and known limitations.
- [ ] Decide on licensing before public release.

## Scope control

Do not implement every phase in one change. Keep the main branch buildable. Prefer a small tested vertical slice over broad scaffolding that has never been run.
