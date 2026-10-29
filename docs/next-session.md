# Next session: validate starter feed sources

## Current state

- Repository: https://github.com/spradlev/scholarly-feed
- The repository contains a Kotlin + Jetpack Compose Android app with feed and source-management screens.
- The debug APK builds with Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00.
- Sources are added only after the app successfully fetches and parses a user-entered HTTPS RSS/Atom URL; the source label, parsed title, level, URL, and verification time persist in Room.
- The source-management screen includes a local searchable directory of publisher candidates. Selecting a candidate fills its name and source level; the user still supplies and verifies the feed URL. The directory is not live web search, and its entries are not verified feed endpoints.
- Discovery can hand a query to the device's web search app with “RSS feed” appended. Search results are not imported into Scholarly Feed; users must manually enter and verify the exact feed URL.
- Refresh fetches article metadata, sorts dated articles newest-first, and keeps undated items at the end. Sources and fetched entries/excerpts persist in Room. Successful refreshes replace only that source's entries; failures keep the existing cache.
- Duplicate entries within a feed are removed when they share a GUID or normalized HTTP(S) URL (URL fragments are ignored). Distinct links and entries without stable identifiers remain separate; separate feeds keep their own source attribution.
- Tapping a cached feed entry opens a detail view with its source, publication date, feed excerpt, and a link to the original web article when available. Full article text is not cached.
- Focus Mode filters the displayed feed to Level 1 research and scientific reporting; turning it off restores entries from all source levels without changing cached data.
- Each source shows the timestamp of its last successful refresh. Failed refreshes leave both the saved articles and timestamp unchanged.
- A file-backed instrumentation test reopens the Room cache and displays a cached article detail with a fetcher that fails if called, verifying feed and detail reading does not require network access.
- The source screen imports and exports OPML. Exports preserve app source levels with the `scholarlyFeedLevel` outline attribute; imported feeds without that attribute default to Level 1, and all imported URLs are verified before being saved.
- Direct fetches include redirect, timeout, size, content-type, and destination-address checks. The app has not yet been tested against a real publisher feed URL.
- Instrumentation tests cover Focus Mode state, verified-source UI flow, Room source persistence, RSS/Atom parsing and deduplication, and URL/address policy; Android lint passes.
- The app has been run on the Medium Phone API 37 emulator.
- TalkBack exposes the Focus Mode label/state on focus, but dynamic state-change announcements were not observed.
- The user wants step-by-step guidance and to learn while building.

## Goal

Create a small starter OPML set from candidate publishers and validate that each selected HTTPS feed endpoint still parses correctly.

## Steps

1. Select a small, clearly documented set of publishers and their feed endpoints.
2. Verify each endpoint against the app's HTTPS, redirect, size, content-type, and address policies.
3. Add only feeds that parse reliably to the starter OPML and document unverified candidates separately.

## Important

- Do not say the build works until it has been run successfully.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- Starter source names are candidates only; no feed endpoint has yet been verified against a live publisher.
- Article deduplication is scoped to one feed response; do not combine articles across publishers because source identity and Focus Mode attribution must remain intact.
- In-app web search is not implemented; external search results require manual feed URL entry and verification.
- The documented minimum SDK is API 26; revisit it before release.
