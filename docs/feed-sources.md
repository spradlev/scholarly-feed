# Feed source policy and validation ledger

## Source levels

| Level | Purpose | Starter candidates |
|---|---|---|
| 1 | Raw academic/scientific reporting and research | EurekAlert, Phys.org, ScienceDaily, Nature; investigate arXiv, bioRxiv, PubMed, and journal tables of contents |
| 2 | Academic commentary and analysis | The Conversation, Quanta Magazine, Arts & Letters Daily |
| 3 | National/global civic affairs | NPR |
| 4 | Local/regional civic affairs | South Seattle Emerald, KUOW, Washington State Standard |

These are source candidates, not confirmed feed endpoints. Do not ship guessed URLs as verified. Some outlets primarily publish press releases or summaries; link to the underlying paper or DOI when available and make the source type clear.

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

## Runtime validation for suggested sources

Before enabling the Add action:

1. Accept only supported URL schemes (normally HTTPS; allow HTTP only if there is a documented reason).
2. Validate safely on the server if the app backend fetches the URL.
3. Prevent SSRF, including private/loopback/link-local destinations and unsafe redirect targets.
4. Apply timeouts, redirect limits, response-size limits, and content-type checks.
5. Parse the response as RSS or Atom and require at least a valid feed document.
6. Show the actual resolved feed title/URL and a clear validation result.
7. Only then enable Add; preserve a way to cancel.

Do not rely on client-only validation as a security boundary.

## Research quality

- Press releases can overstate or simplify findings.
- Where practical, link to the underlying study, journal article, preprint, or DOI.
- Keep source identity visible.
- The Librarian should state whether it has an abstract/excerpt or full text and must not invent details absent from that text.
- Reuters is currently excluded based on the project's prior notes that it has no public RSS feed. This is a project assumption to revisit only if necessary; do not invent a feed URL.

## Starter ledger

Use a separate issue or table to record per-feed verification before release. Until tested, label all candidate URLs “unverified.”
