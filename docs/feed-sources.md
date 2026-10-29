# Feed source policy and validation ledger

## Source levels

| Level | Purpose | Starter candidates |
|---|---|---|
| 1 | Raw academic/scientific reporting and research | EurekAlert, Phys.org, ScienceDaily, Nature; investigate arXiv, bioRxiv, PubMed, and journal tables of contents |
| 2 | Academic commentary and analysis | The Conversation, Quanta Magazine, Arts & Letters Daily |
| 3 | National/global civic affairs | NPR |
| 4 | Local/regional civic affairs | South Seattle Emerald, KUOW, Washington State Standard |

These are source candidates, not confirmed feed endpoints. Do not ship guessed URLs as verified. The current app lets a user enter an exact URL and adds it only after an HTTPS request returns a parseable RSS/Atom feed with a title. No starter candidate feed has yet been verified in this project. Some outlets primarily publish press releases or summaries; link to the underlying paper or DOI when available and make the source type clear.

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

## Starter ledger

Use a separate issue or table to record per-feed verification before release. Until tested, label all candidate URLs “unverified.”
