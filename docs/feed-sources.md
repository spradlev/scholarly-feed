# Feed source policy and validation ledger

## Source levels

| Level | Purpose | Starter candidates |
|---|---|---|
| 1 | Raw academic/scientific reporting and research | EurekAlert, Phys.org, ScienceDaily, Nature; investigate arXiv, bioRxiv, PubMed, and journal tables of contents |
| 2 | Academic commentary and analysis | The Conversation, Quanta Magazine, Arts & Letters Daily |
| 3 | National/global civic affairs | NPR |
| 4 | Local/regional civic affairs | South Seattle Emerald, KUOW, Washington State Standard |

These are source candidates, not confirmed feed endpoints unless listed in the validation ledger below. The app lets a user enter an exact URL and adds it only after an HTTPS request returns a parseable RSS/Atom feed with a title. Some outlets primarily publish press releases or summaries; link to the underlying paper or DOI when available and make the source type clear.

The app's built-in Discovery directory contains publisher names and topic keywords from this candidate list. Searching it is local-only; it does not claim that a publisher has a supported feed. Discovery can hand the search phrase to the device's web search app with “RSS feed” appended. Search results are not imported or trusted automatically: the user must enter and verify an exact HTTPS feed URL.

## Validation requirements

For every feed URL, record:

- Publisher and feed title.
- Exact feed URL.
- Date tested.
- HTTP outcome and final URL after redirects.
- Whether the response is RSS or Atom and parses successfully.
- Whether representative entries have stable IDs/links and publication timestamps.
- Whether entries contain full text, abstracts, or excerpts.
- Any access, redirect, rate-limit, or content-type caveats.

A feed is not validated just because its home page opens or because a URL appears in a search result.

## Runtime validation for user-entered sources

Before enabling the Add action:

1. Accept HTTPS only, with no embedded credentials.
2. Resolve and reject local, private, link-local, and reserved IP destinations; repeat validation after each redirect.
3. Apply connection/read/call timeouts, a redirect limit, a 2 MiB response limit, and an XML/feed content-type allowlist.
4. Parse the response as RSS or Atom and require a non-empty feed title.
5. Show the parsed feed title and a clear success result after verification succeeds; save only the verified feed.

The current checks run on-device and are defense-in-depth for user-entered URLs, not a server-side security boundary. Do not reuse this client-only design for arbitrary remote suggestions or a public proxy without adding server-side egress controls and independent security review.

## Research quality

- Press releases can overstate or simplify findings.
- Where practical, link to the underlying study, journal article, preprint, or DOI.
- Keep source identity visible.
- The Librarian should state whether it has an abstract/excerpt or full text and must not invent details absent from that text.
- Reuters is currently excluded based on the project's prior notes that it has no public RSS feed. This is a project assumption to revisit only if necessary; do not invent a feed URL.

## Starter set and validation ledger

The starter OPML file is [`starter-sources.opml`](starter-sources.opml). Its four URL-only entries cover one source at each level. The feed endpoints were fetched on 2026-10-28 and then passed through the Android app's `SafeFeedFetcher` on the API 37 emulator. That runtime check applies the app's HTTPS, redirect, public-address, timeout, response-size, content-type, and RSS/Atom parser rules. Every feed produced a non-empty title and entries with links and parsed publication timestamps.

| Publisher | Level | Feed title | Exact URL | HTTP / type | Entry data and caveats |
|---|---:|---|---|---|---|
| ScienceDaily | 1 | Latest Science News -- ScienceDaily | `https://www.sciencedaily.com/rss/all.xml` | 200, `application/xml`; RSS | Linked entries and publication dates parsed. Feed descriptions are summaries; full article pages are not fetched. |
| The Conversation (US) | 2 | The Conversation -- Articles (US) | `https://theconversation.com/us/articles.atom` | 200, `application/atom+xml`; Atom | Stable Atom entry IDs, alternate links, and published timestamps parsed. Entries provide HTML content; treat it as feed-provided content, not a guarantee of full article text. |
| NPR News | 3 | NPR Topics: News | `https://feeds.npr.org/1001/rss.xml` | 200, `text/xml`; RSS | Linked entries and publication dates parsed. Descriptions provide feed summaries; NPR marks its feed for personal use. |
| Washington State Standard | 4 | Washington State Standard | `https://washingtonstatestandard.com/feed/` | 200, `application/rss+xml`; RSS | Linked entries and publication dates parsed. Feed descriptions/content are publisher-provided; the app does not fetch article pages. |

These are a tested snapshot, not a guarantee of future uptime or publisher policy. Import still re-verifies every URL before saving it. The other candidate endpoints remain unverified and are intentionally absent from the starter OPML.
