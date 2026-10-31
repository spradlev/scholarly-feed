# Feed source policy and validation ledger

## Source levels

| Level | Purpose | Starter candidates |
|---|---|---|
| 1 | Raw academic/scientific reporting and research | EurekAlert, Phys.org, ScienceDaily, Nature; investigate arXiv, bioRxiv, PubMed, and journal tables of contents |
| 2 | Academic commentary and analysis | The Conversation, Quanta Magazine, Arts & Letters Daily |
| 3 | National/global civic affairs | NPR |
| 4 | Local/regional civic affairs | South Seattle Emerald, KUOW, Washington State Standard |

These are source candidates, not confirmed feed endpoints unless listed in the validation ledger below. The app lets a user enter an exact URL and adds it only after an HTTPS request returns a parseable RSS/Atom feed with a title. Some outlets primarily publish press releases or summaries; link to the underlying paper or DOI when available and make the source type clear.

The app's built-in Discovery directory contains concise publisher names, topic keywords, and a suggested feed URL for each listed entry. These suggestions are limited to endpoints that returned feed-shaped responses in the latest live check; they remain candidates, not app-verified sources. Selecting one fills both the title and URL fields. The user must still verify the exact HTTPS URL in the app before it is saved. Discovery can also hand the search phrase to the device's web search app with “RSS feed” appended; search results are not imported or trusted automatically.

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
3. Apply connection/read/call timeouts, a redirect limit, a 4 MiB response limit, and an XML/feed content-type allowlist.
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

The starter OPML file is [`starter-sources.opml`](starter-sources.opml). Its six URL-only entries cover one source at Levels 2–4 and three Level 1 sources. The original four feed endpoints were fetched on 2026-10-28 and then passed through the Android app's `SafeFeedFetcher` on the API 37 emulator. The two arXiv endpoints were validated the same way on 2026-10-29. These runtime checks apply the app's HTTPS, redirect, public-address, timeout, response-size, content-type, and RSS/Atom parser rules. Every listed feed produced a non-empty title and entries with links and parsed publication timestamps.

| Publisher | Level | Feed title | Exact URL | HTTP / type | Entry data and caveats |
|---|---:|---|---|---|---|
| ScienceDaily | 1 | Latest Science News -- ScienceDaily | `https://www.sciencedaily.com/rss/all.xml` | 200, `application/xml`; RSS | Linked entries and publication dates parsed. Feed descriptions are summaries; full article pages are not fetched. |
| arXiv Computer Science | 1 | cs updates on arXiv.org | `https://rss.arxiv.org/rss/cs` | 200, `application/rss+xml`; RSS | Parsed through the app's production fetcher with the 4 MiB cap; current response is about 2.96 MB and has 1,397 entries. Entries include links and publication dates. Includes broad Computer Science topics, including AI. |
| arXiv AI and Machine Learning | 1 | cs.AI updates on arXiv.org | `https://rss.arxiv.org/rss/cs.AI` | 200, `application/rss+xml`; RSS | Parsed through the app's production fetcher; current response is about 0.90 MB and has 407 entries. Entries include links and publication dates. Overlaps with the broader Computer Science feed. |
| The Conversation (US) | 2 | The Conversation -- Articles (US) | `https://theconversation.com/us/articles.atom` | 200, `application/atom+xml`; Atom | Stable Atom entry IDs, alternate links, and published timestamps parsed. Entries provide HTML content; treat it as feed-provided content, not a guarantee of full article text. |
| NPR News | 3 | NPR Topics: News | `https://feeds.npr.org/1001/rss.xml` | 200, `text/xml`; RSS | Linked entries and publication dates parsed. Descriptions provide feed summaries; NPR marks its feed for personal use. |
| Washington State Standard | 4 | Washington State Standard | `https://washingtonstatestandard.com/feed/` | 200, `application/rss+xml`; RSS | Linked entries and publication dates parsed. Feed descriptions/content are publisher-provided; the app does not fetch article pages. |

These are a tested snapshot, not a guarantee of future uptime or publisher policy. Import still re-verifies every URL before saving it. The arXiv Physics and astro-ph endpoints currently return valid but empty feeds and are intentionally absent until they contain entries; other candidate endpoints remain unverified and absent from the starter OPML.

Discovery's suggested URLs are a separate convenience list from the starter set. Nature (`https://www.nature.com/nature.rss`), Quanta Magazine (`https://www.quantamagazine.org/feed/`), and bioRxiv (`https://connect.biorxiv.org/biorxiv_xml.php?subject=all`) returned feed-shaped responses during a live HTTP/XML check on 2026-10-30, as did the six starter endpoints above. These three suggestions have not yet been validated through the Android app's `SafeFeedFetcher`; the app verifies them when a user chooses Verify and add. Other candidate outlets, including APA, Psychology Today, Oxford University Press, and BMJ Journals, are not presented with guessed feed URLs.
