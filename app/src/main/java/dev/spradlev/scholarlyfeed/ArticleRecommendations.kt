package dev.spradlev.scholarlyfeed

private val ARTICLE_WORD = Regex("[\\p{L}\\p{N}]{3,}")
private val COMMON_WORDS = setOf(
    "about",
    "after",
    "also",
    "from",
    "have",
    "into",
    "more",
    "over",
    "that",
    "their",
    "there",
    "these",
    "this",
    "through",
    "under",
    "using",
    "were",
    "what",
    "when",
    "where",
    "which",
    "while",
    "with",
)

internal fun recommendSimilarArticles(
    article: FeedArticle,
    candidates: List<FeedArticle>,
    limit: Int = 4,
): List<FeedArticle> {
    if (limit <= 0) return emptyList()

    val titleTerms = article.title.articleTerms()
    val excerptTerms = article.excerpt.orEmpty().articleTerms()

    return candidates.asSequence()
        .filterNot { it.id == article.id && it.sourceUrl == article.sourceUrl }
        .mapNotNull { candidate ->
            val titleMatches = titleTerms.intersect(candidate.title.articleTerms()).size
            val excerptMatches = excerptTerms.intersect(
                candidate.excerpt.orEmpty().articleTerms(),
            ).size
            val sameSourceBonus = if (candidate.sourceUrl == article.sourceUrl) 2 else 0
            val score = titleMatches * 3 + excerptMatches + sameSourceBonus
            if (score == 0) null else candidate to score
        }
        .sortedWith(
            compareByDescending<Pair<FeedArticle, Int>> { it.second }
                .thenByDescending { it.first.publishedAtMillis ?: Long.MIN_VALUE },
        )
        .take(limit)
        .map { it.first }
        .toList()
}

private fun String.articleTerms(): Set<String> =
    ARTICLE_WORD.findAll(lowercase())
        .map { it.value }
        .filterNot(COMMON_WORDS::contains)
        .toSet()
