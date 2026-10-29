# Next session: choose live source search

## Current state

- Repository: https://github.com/spradlev/scholarly-feed
- The repository contains a Kotlin + Jetpack Compose Android app with feed and source-management screens.
- The debug APK builds with Android Gradle Plugin 9.2.0, Gradle 9.4.1, Kotlin 2.4.10, and Compose BOM 2026.09.00.
- Sources are added only after the app successfully fetches and parses a user-entered HTTPS RSS/Atom URL; the source label, parsed title, level, URL, and verification time persist in Room.
- The source-management screen includes a local searchable directory of publisher candidates. Selecting a candidate fills its name and source level; the user still supplies and verifies the feed URL. The directory is not live web search, and its entries are not verified feed endpoints.
- Refresh fetches current article metadata, sorts dated articles newest-first, and keeps undated items at the end. Article results remain in memory only; there is no offline article cache.
- Direct fetches include redirect, timeout, size, content-type, and destination-address checks. The app has not yet been tested against a real publisher feed URL.
- Instrumentation tests cover Focus Mode state, verified-source UI flow, Room source persistence, RSS/Atom parsing, and URL/address policy; Android lint passes.
- The app has been run on the Medium Phone API 37 emulator.
- TalkBack exposes the Focus Mode label/state on focus, but dynamic state-change announcements were not observed.
- The user wants step-by-step guidance and to learn while building.

## Goal

Before implementing live web search, choose a search provider and define how candidate results will be labeled, linked, and verified. Do not add a remote search integration or invent feed URLs until that decision is made.

## Steps

1. Compare a managed search API with handing off to an external browser search.
2. Keep any result as an unverified publisher/feed candidate until its exact RSS/Atom URL is verified by the app.
3. Add the smallest integration slice and tests after the product decision.

## Important

- Do not say the build works until it has been run successfully.
- Never commit API keys, signing credentials, local machine secrets, or personal data.
- Starter source names are candidates only; no feed endpoint has yet been verified against a live publisher.
- The documented minimum SDK is API 26; revisit it before release.
