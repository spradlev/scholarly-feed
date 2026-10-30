# Proposed architecture

This document records the intended direction and distinguishes it from the current implementation.

## Current implementation

- **UI:** Compose feed and source-management screens; source operations are invoked through a small repository.
- **Sources and feed entries:** Room stores verified source metadata and fetched article metadata/excerpts. Each entry belongs to a source and is replaced transactionally only after that source fetch succeeds; source removal cascades to its entries.
- **Feed ingestion:** OkHttp fetches user-entered HTTPS URLs with manual redirect checks, timeouts, a 4 MiB response cap, content-type checks, and public-address DNS filtering. RSS Parser handles RSS/Atom parsing and article ordering.
- **Article content:** Only feed-provided metadata and excerpts are cached. Cached articles have a detail view; full article text extraction is not implemented.
- **Refresh status:** Each source stores the time of its last successful fetch. Article replacement and timestamp updates commit together, so failed fetches preserve both.
- **OPML:** Source list interchange uses OPML 2.0; `scholarlyFeedLevel` preserves source levels, while standard OPML without it defaults to Level 1. Import must route every URL through existing feed verification before persistence.
- **Article identity:** Duplicate entries within a feed are removed when they share a GUID or HTTP(S) URL (ignoring URL fragments). Entries without either stable identifier remain distinct; different feeds retain their own source attribution.
- **Not implemented:** ViewModels and the Librarian. Google Drive source-list sync is being integrated; it has not yet been validated against a configured OAuth project or live Drive account.

The Room database upgrades version 1 source-only installations to version 2 without dropping their saved sources. The network safeguards run on-device and are not a substitute for server-side egress controls if the architecture later accepts arbitrary remote URLs.

## Initial application

Start with one Android application module using Kotlin and Jetpack Compose. Keep the first commit minimal and buildable. Avoid adding a backend or persistence library until the starter app builds.

### Suggested layers

- **UI:** Compose screens and native accessible controls.
- **Presentation:** ViewModels exposing observable UI state and handling user actions.
- **Domain:** Feed/article/source models and rules for chronological ordering, Focus Mode, source levels, and duplicate handling.
- **Data:** Repositories that coordinate local storage and refreshes.
- **Local persistence:** Room for sources, cached feed entries/articles, saved state, and refresh metadata; DataStore for small preferences where appropriate.
- **Feed ingestion:** HTTP fetching plus an RSS/Atom parser selected after maintenance and compatibility review.
- **OPML:** import/export and starter-pack creation independent of screens.
- **Librarian client:** a small typed API boundary to a server-side proxy. AI provider selection stays behind that service.
- **Proxy:** fetch/validate feeds where needed and call the selected LLM provider without exposing API keys to the app.

## Core data concepts

Potential models; refine before coding:

- `FeedSource`: stable ID, title, URL, level/category, enabled state, optional user-defined label, last successful refresh, last error.
- `FeedEntry`: stable ID, source ID, title, link, published timestamp (nullable), updated timestamp (nullable), author, plain-text summary/excerpt, fetched timestamp, read/saved state.
- `ArticleContent`: entry ID, plain-text extracted content when available, retrieval timestamp, and extraction status.
- `RefreshResult`: success/failure plus counts and user-facing status; failure must not delete cached content.
- `LibrarianRequest`: article identity, available article text/excerpt, user question, and explicit context limits.
- `SuggestedSource`: candidate URL/title/category plus validation state; the Add action is unavailable until validation succeeds.

Do not assume every RSS item has full article text. Keep excerpt and full-text content distinct.

## Chronological ordering

The normal feed view is newest-first. Sort by valid publication timestamp; define an explicit deterministic policy for entries without a valid timestamp (for example, place them in a clearly identified undated group after dated entries). Never silently substitute the current time for a missing publication date. Preserve stable ordering for equal timestamps.

## Focus Mode

Focus Mode is a filter, not a ranking algorithm. It should exclude Levels 3 and 4 and any source classified as non-academic. Make its current state visible to assistive technology. The source model should support a source-level category and an academic/non-academic classification; avoid relying only on fragile display-name string matching.

## Offline behavior

- Previously fetched source definitions and articles remain available without a network.
- Refreshes update local data transactionally or in a way that does not wipe good cached data on failure.
- The UI distinguishes cached content from a failed refresh.
- AI requests may require connectivity; explain that clearly and never block ordinary reading on AI availability.

## Google Drive source synchronization

The initial optional sync target is Google Drive's private `appDataFolder`, requested with the `drive.appdata` scope. Sync is explicit and manual. The app stores only the source list, encoded as OPML; cached articles and excerpts remain local, and a user can continue using the app without connecting Drive.

Feed URL is the source identity. Sync merges the two lists and never propagates deletions. If a matching URL has different source title or level metadata, ask which metadata to keep; local values are the default. Every source present only in Drive must pass the existing HTTPS feed verification before being stored locally. If verification fails for any remote-only source, retain any successfully verified local additions, report the failures, and leave the Drive file unchanged.

An entry removed from one side is retained from the other side during merge; deletions are not propagated. The app must not persist OAuth access tokens. Disconnect revokes this app's Drive access but does not delete the private Drive copy. OAuth setup and live authorization/sync still require a configured Google Cloud project, enabled Drive API, and an Android OAuth client matching the app's package and signing certificate. Never commit OAuth client secrets or tokens.

## Librarian grounding

Send only the article content actually available to the app, alongside title/source/link and a clear indication of whether the content is an excerpt or full text. Instruct the model to:
- answer from provided content first;
- identify when the content does not answer the question;
- mark any outside context as outside the article;
- distinguish study findings, author interpretation, and press-release framing;
- avoid inventing methods, results, limitations, or citations.

The client should display a short source/context note, such as “Based on the feed excerpt” or “Includes outside context,” when applicable.

## Future portability

Keep domain models, OPML semantics, source classification, and the Librarian request/response contract independent of Compose. Do not prematurely build a cross-platform abstraction layer; just avoid coupling core rules to UI widgets.
