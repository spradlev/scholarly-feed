# Proposed architecture

This document records the intended direction and distinguishes it from the current implementation.

## Current implementation

- **UI:** Compose feed and source-management screens; source operations are invoked through a small repository.
- **Sources and feed entries:** Room stores verified source metadata and fetched article metadata/excerpts. Each entry belongs to a source and is replaced transactionally only after that source fetch succeeds; source removal cascades to its entries.
- **Feed ingestion:** OkHttp fetches user-entered HTTPS URLs with manual redirect checks, timeouts, a 2 MiB response cap, content-type checks, and public-address DNS filtering. RSS Parser handles RSS/Atom parsing and article ordering.
- **Article content:** Only feed-provided metadata and excerpts are cached. Cached articles have a detail view; full article text extraction is not implemented.
- **Refresh status:** Each source stores the time of its last successful fetch. Article replacement and timestamp updates commit together, so failed fetches preserve both.
- **Not implemented:** ViewModels, OPML, article detail, deduplication, Focus Mode feed filtering, and the Librarian.

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

## Future synchronization

Local-only storage remains a supported option. A later sync feature may let the user opt into a cloud storage provider such as Google Drive, Box, Dropbox, or OneDrive. Provider integrations, account linking, sync scope, conflict handling, and remote data format are not selected or implemented; keep local storage usable without connecting any cloud account.

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
