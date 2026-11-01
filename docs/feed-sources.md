# Feed source policy and validation ledger

## Source levels

| Level | Purpose | Starter coverage |
|---|---|---|
| 1 | Research, scientific reporting, and clinical medicine | ScienceDaily, Nature, MIT Technology Review, NEJM, MedPage Today, Nature Medicine, PLOS Medicine, PLOS One |
| 2 | Academic analysis, technology, ecology, and commentary | The Conversation, Psychology Today, Hackaday, Nautilus, Ars Technica, Yale Environment 360, Mongabay, Scientific American, Brain & Behavior Research Foundation |
| 3 | Global affairs, public health, nature, and outdoor recreation | NPR, BBC World News, Reddit World News, ProPublica, BIKEPACKING.com, Audubon, High Country News, Outside Online, MedlinePlus, The Radavist |
| 4 | Local civic and urban news | Washington State Standard, The Seattle Times, The Urbanist, Cascade PBS |

These are source candidates, not endorsements of every article. The app adds an endpoint only after an HTTPS request returns a parseable RSS/Atom feed with a title. Some outlets publish press releases, summaries, abstracts, or advocacy content rather than peer-reviewed papers; link to the underlying paper or DOI when available and make the source identity clear.

The app's built-in Discovery directory is separate from the starter set. Its suggestions are candidates, not verified feeds; selecting one fills the title and URL fields, and the user still verifies the exact HTTPS URL before saving it. Discovery can also hand the search phrase to the device's web search app with “RSS feed” appended; search results are not imported or trusted automatically.

## Validation requirements

For every feed URL, record the publisher and feed title, exact URL, test date, HTTP outcome and redirect destination, RSS/Atom format, parse result, representative links/dates, content type, and any access caveats. A feed is not validated just because its home page opens or because a URL appears in a search result.

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
- Journal feeds may provide abstracts or tables of contents, not full-text papers.
- Where practical, link to the underlying study, journal article, preprint, or DOI.
- Keep source identity visible.
- The Librarian should state whether it has an abstract/excerpt or full text and must not invent details absent from that text.
- Reuters remains excluded based on the project's prior notes that it has no public RSS feed; do not invent a feed URL.

## Starter set and validation ledger

The bundled asset and downloadable OPML are [`starter-sources.opml`](starter-sources.opml). They contain 31 URL-only feeds across Levels 1–4 (8, 9, 10, and 4 respectively). On 2026-11-01, each URL passed the Android app's production `SafeFeedFetcher` on a Galaxy S21 running Android 14: HTTPS/address/redirect policy, 4 MiB response cap, allowed XML content type, feed parsing, non-empty article entries, and at least one linked entry. The table records the live response type and item count at that check. Publication timestamps vary by source; missing dates are supported and appear last in the feed. Availability and counts change; starter import re-verifies every URL and reports any failures. RSS/Atom descriptions are publisher-provided and are not guaranteed to be full text.

| Publisher | Level | Parsed feed title | Exact URL | HTTP / type / entries | Notes |
|---|---:|---|---|---|---|
| MIT Technology Review | 1 | MIT Technology Review | `https://www.technologyreview.com/feed/` | 200, RSS, 10 | Feed descriptions; article pages are not fetched unless requested in reader mode. |
| MedPage Today | 1 | MedPageToday.com - medical news for physicians | `https://www.medpagetoday.com/rss/headlines.xml` | 200, RSS, 20 | Medical news reporting, not a substitute for clinical guidance. |
| Nature Medicine | 1 | Nature Medicine | `https://www.nature.com/nm.rss` | 200, RSS 1.0/RDF, 8 | Journal feed; may provide abstracts or article metadata rather than full text. Redirects remained HTTPS. |
| Nature News | 1 | Nature | `https://www.nature.com/nature.rss` | 200, RSS 1.0/RDF, 75 | Publisher news feed; redirects remained HTTPS. |
| New England Journal of Medicine | 1 | massmed: New England Journal of Medicine: Table of Contents | `https://www.nejm.org/action/showFeed?jc=nejm&type=etoc&feed=rss` | 200, RSS 1.0/RDF, 57 | Table of contents; full text may require publisher access. |
| PLOS Medicine | 1 | PLOS Medicine | `https://journals.plos.org/plosmedicine/feed/atom` | 200, Atom, 30 | Peer-reviewed journal feed; entries may contain abstracts and article links. |
| PLOS One | 1 | PLOS One | `https://journals.plos.org/plosone/feed/atom` | 200, Atom, 30 | Broad peer-reviewed science; entries may contain abstracts and article links. |
| ScienceDaily | 1 | Latest Science News -- ScienceDaily | `https://www.sciencedaily.com/rss/all.xml` | 200, RSS, 60 | Science news and research summaries; check linked study for primary-source context. |
| Ars Technica | 2 | Ars Technica - All content | `https://feeds.arstechnica.com/arstechnica/index` | 200, RSS, 20 | Technology journalism. |
| Brain & Behavior Research Foundation | 2 | Brain & Behavior Research Foundation | `https://www.bbrfoundation.org/rss.xml` | 200, RSS, 10 | Research-focused mental-health organization; distinguish research summaries from journal articles. |
| Hackaday | 2 | Hackaday | `https://hackaday.com/feed/` | 200, RSS, 7 | Engineering and technology reporting. |
| Mongabay | 2 | Conservation news | `https://news.mongabay.com/feed/` | 200, RSS, 32 | Environmental and conservation journalism. |
| Nautilus Science & Culture | 2 | Nautilus | `https://nautil.us/feed/` | 200, RSS, 10 | Science and culture reporting. |
| Psychology Today | 2 | Psychology Today: The Latest | `https://www.psychologytoday.com/us/front/feed` | 200, RSS, 30 | Psychology reporting and commentary; not a peer-reviewed journal feed. |
| Scientific American | 2 | Scientific American | `https://www.scientificamerican.com/platform/syndication/rss/` | 200, RSS, 50 | Corrected from the supplied 404 endpoint; science journalism. |
| The Conversation (US) | 2 | The Conversation – Articles (US) | `https://theconversation.com/us/articles.atom` | 200, Atom, 50 | Academic-authored analysis; feed content is not necessarily the complete article. |
| Yale Environment 360 | 2 | Yale E360 | `https://e360.yale.edu/feed.xml` | 200, RSS, 100 | Corrected from the supplied 404 endpoint; environmental reporting and analysis. |
| BBC World News | 3 | BBC News | `https://feeds.bbci.co.uk/news/world/rss.xml` | 200, RSS, 30 | HTTPS endpoint; the supplied HTTP URL was excluded because app imports require HTTPS. |
| BIKEPACKING.com | 3 | BIKEPACKING.com | `https://bikepacking.com/feed/` | 200, RSS, 61 | Outdoor recreation and bikepacking journalism. |
| High Country News | 3 | High Country News | `https://www.hcn.org/feed` | 200, RSS, 10 | Western U.S. and environmental reporting. |
| MedlinePlus: New Links | 3 | New Links on MedlinePlus | `https://medlineplus.gov/groupfeeds/new.xml` | 200, RSS, 87 | Official health-information links recently added to MedlinePlus; this is not a breaking-health-news feed. |
| NPR News | 3 | NPR Topics: News | `https://feeds.npr.org/1001/rss.xml` | 200, RSS, 10 | NPR marks its feeds for personal use; follow publisher terms. |
| National Audubon Society | 3 | Audubon News | `https://www.audubon.org/rss.xml` | 200, RSS, 25 | Corrected from the supplied 404 endpoint; bird and conservation coverage. |
| Outside Online | 3 | Outside Online | `https://www.outsideonline.com/feed/` | 200, RSS, 10 | Redirects to a feed URL with an anonymous-access query parameter. |
| ProPublica | 3 | ProPublica | `https://www.propublica.org/feeds/propublica/main` | 200, RSS, 20 | Investigative journalism. |
| Reddit: World News | 3 | World News | `https://www.reddit.com/r/worldnews/.rss` | 200, Atom, 25 | Community-curated links, not an editorial newsroom feed. |
| The Radavist | 3 | The Radavist | `https://theradavist.com/feed/` | 200, RSS, 20 | Cycling and outdoor culture reporting. |
| Cascade PBS | 4 | Cascade PBS | `https://www.cascadepbs.org/rss/` | 200, RSS, 15 | Corrected from the supplied URL; regional public-media reporting. |
| The Seattle Times | 4 | The Seattle Times The Seattle Times | `https://www.seattletimes.com/feed/` | 200, RSS, 32 | Regional news; some content may require a subscription. |
| The Urbanist | 4 | The Urbanist | `https://www.theurbanist.org/rss/` | 200, RSS, 15 | Puget Sound urbanism, transportation, and civic policy. |
| Washington State Standard | 4 | Washington State Standard | `https://washingtonstatestandard.com/feed/` | 200, RSS, 100 | Washington state public-policy reporting. |

The following supplied entries are intentionally not in the starter set: the JAMA and NIMH URLs returned 404; both arXiv feeds returned valid but empty responses during this check; Sierra Club returned 403; South Seattle Emerald returned a valid but empty feed; the AP URL is a third-party FeedX mirror whose official provenance was not established. These sources can be reconsidered when a working, appropriately sourced endpoint is verified. The BBC endpoint was switched to its HTTPS equivalent. Scientific American, Yale Environment 360, Audubon, MedlinePlus, and Cascade PBS use corrected endpoints.

These measurements are a tested snapshot, not a guarantee of future uptime or publisher policy. Import still re-verifies every URL before saving it.

Discovery's suggested URLs remain a separate convenience list from the starter set. Nature (`https://www.nature.com/nature.rss`), Quanta Magazine (`https://www.quantamagazine.org/feed/`), and bioRxiv (`https://connect.biorxiv.org/biorxiv_xml.php?subject=all`) returned feed-shaped responses during a live HTTP/XML check on 2026-10-30. These Discovery-only suggestions were not validated through the Android app's `SafeFeedFetcher`; the app verifies them when a user chooses Verify and add. Other candidate outlets, including APA, Psychology Today, Oxford University Press, and BMJ Journals, are not presented with guessed feed URLs.
