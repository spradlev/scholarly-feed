# Next session: cache fetched articles

## Current state

- Repository: https://github.com/spradlev/scholarly-feed
- The repository contains a Kotlin + Jetpack Compose Android app with feed and source-management screens.
- The debug APK builds with Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00.
- Sources are added only after the app successfully fetches and parses a user-entered HTTPS RSS/Atom URL; the source label, parsed title, level, URL, and verification time persist in Room.
- The source-management screen includes a local searchable directory of publisher candidates. Selecting a candidate fills its name and source level; the user still supplies and verifies the feed URL. The directory is not live web search, and its entries are not verified feed endpoints.
- Discovery can hand a query to the device's web search app with “RSS feed” appended. Search results are not imported into Scholarly Feed; users must manually enter and verify the exact feed URL.
- Refresh fetches article metadata, sorts dated articles newest-first, and keeps undated items at the end. Sources and fetched entries/excerpts persist in Room. Successful refreshes replace only that source's entries; failures keep the existing cache.
- Direct fetches include redirect, timeout, size, content-type, and destination-address checks. The app has not yet been tested against a real publisher feed URL.
- Instrumentation tests cover Focus Mode state, verified-source UI flow, Room source persistence, RSS/Atom parsing, and URL/address policy; Android lint passes.
- The app has been run on the Medium Phone API 37 emulator.
- TalkBack exposes the Focus Mode label/state on focus, but dynamic state-change announcements were not observed.
- The user wants step-by-step guidance and to learn while building.

## Goal

Continue with a small, tested feature: add an article detail view for cached feed entries, clearly distinguishing feed excerpts from full article text.

## Steps

1. Make a cached entry open a readable detail view.
2. Display the source, link, publication date, and any supplied excerpt.
3. Keep unavailable full text clearly labeled; do not imply an excerpt is the full article.
4. Add UI coverage and verify the detail view from cached data without a network request.

## Important

- Do not say the build works until it has been run successfully.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- Starter source names are candidates only; no feed endpoint has yet been verified against a live publisher.
- In-app web search is not implemented; external search results require manual feed URL entry and verification.
- The documented minimum SDK is API 26; revisit it before release.
