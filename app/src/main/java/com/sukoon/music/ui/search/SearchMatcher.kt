package com.sukoon.music.ui.search

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

/**
 * Utility for normalized, token-aware, and typo-tolerant local search matching.
 */
object SearchMatcher {
    private val punctuationRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private val multiSpaceRegex = Regex("\\s+")

    private const val MIN_SCORE_THRESHOLD = 80

    data class QueryContext(
        val normalizedQuery: String,
        val queryTokens: List<String>
    )

    data class MatchResult(
        val score: Int,
        val directMatch: Boolean
    ) {
        val isMatch: Boolean
            get() = directMatch || score >= MIN_SCORE_THRESHOLD
    }

    fun createQueryContext(query: String): QueryContext {
        val normalized = normalize(query)
        return QueryContext(
            normalizedQuery = normalized,
            queryTokens = tokenize(normalized)
        )
    }

    fun scoreSong(
        context: QueryContext,
        title: String,
        artist: String,
        album: String
    ): MatchResult {
        if (context.normalizedQuery.isBlank()) {
            return MatchResult(score = 0, directMatch = false)
        }

        val titleNormalized = normalize(title)
        val artistNormalized = normalize(artist)
        val albumNormalized = normalize(album)

        val titleTokens = tokenize(titleNormalized)
        val artistTokens = tokenize(artistNormalized)
        val albumTokens = tokenize(albumNormalized)

        val titleScore = scoreField(
            field = titleNormalized,
            fieldTokens = titleTokens,
            context = context,
            exactWeight = 1200,
            prefixWeight = 700,
            containsWeight = 260,
            tokenExactWeight = 110,
            tokenPrefixWeight = 65,
            fuzzyWeight = 55
        )

        val artistScore = scoreField(
            field = artistNormalized,
            fieldTokens = artistTokens,
            context = context,
            exactWeight = 900,
            prefixWeight = 500,
            containsWeight = 180,
            tokenExactWeight = 90,
            tokenPrefixWeight = 55,
            fuzzyWeight = 45
        )

        val albumScore = scoreField(
            field = albumNormalized,
            fieldTokens = albumTokens,
            context = context,
            exactWeight = 650,
            prefixWeight = 350,
            containsWeight = 120,
            tokenExactWeight = 70,
            tokenPrefixWeight = 40,
            fuzzyWeight = 30
        )

        val aggregatedCoverageScore = coverageBoost(
            context = context,
            titleTokens = titleTokens,
            artistTokens = artistTokens,
            albumTokens = albumTokens
        )

        val totalScore = titleScore.score + artistScore.score + albumScore.score + aggregatedCoverageScore
        val directMatch = titleScore.directMatch || artistScore.directMatch || albumScore.directMatch

        return MatchResult(score = totalScore, directMatch = directMatch)
    }

    fun normalize(value: String): String {
        if (value.isBlank()) return ""

        val lowered = value.lowercase(Locale.ROOT).trim()
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val withoutDiacritics = decomposed.filterNot { ch ->
            Character.getType(ch) == Character.NON_SPACING_MARK.toInt()
        }

        return withoutDiacritics
            .replace(punctuationRegex, " ")
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    private fun tokenize(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return value
            .split(' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private data class FieldScore(
        val score: Int,
        val directMatch: Boolean
    )

    private fun scoreField(
        field: String,
        fieldTokens: List<String>,
        context: QueryContext,
        exactWeight: Int,
        prefixWeight: Int,
        containsWeight: Int,
        tokenExactWeight: Int,
        tokenPrefixWeight: Int,
        fuzzyWeight: Int
    ): FieldScore {
        if (field.isBlank()) return FieldScore(score = 0, directMatch = false)

        var score = 0
        var directMatch = false

        when {
            field == context.normalizedQuery -> {
                score += exactWeight
                directMatch = true
            }
            field.startsWith(context.normalizedQuery) -> {
                score += prefixWeight
                directMatch = true
            }
            field.contains(context.normalizedQuery) -> {
                score += containsWeight
                directMatch = true
            }
        }

        context.queryTokens.forEach { token ->
            if (token.length < 2) return@forEach

            when {
                fieldTokens.any { it == token } -> {
                    score += tokenExactWeight
                    directMatch = true
                }
                fieldTokens.any { it.startsWith(token) } -> {
                    score += tokenPrefixWeight
                    directMatch = true
                }
                token.length >= 4 && fieldTokens.any { candidate ->
                    isFuzzyTokenMatch(token, candidate)
                } -> {
                    score += fuzzyWeight
                }
            }
        }

        return FieldScore(score = score, directMatch = directMatch)
    }

    private fun coverageBoost(
        context: QueryContext,
        titleTokens: List<String>,
        artistTokens: List<String>,
        albumTokens: List<String>
    ): Int {
        if (context.queryTokens.isEmpty()) return 0

        val allTokens = (titleTokens + artistTokens + albumTokens).distinct()
        var matchedTokenCount = 0

        context.queryTokens.forEach { token ->
            if (token.length < 2) return@forEach

            val matched = allTokens.any { candidate ->
                candidate == token ||
                    candidate.startsWith(token) ||
                    (token.length >= 4 && isFuzzyTokenMatch(token, candidate))
            }
            if (matched) matchedTokenCount++
        }

        if (matchedTokenCount == 0) return 0

        var boost = matchedTokenCount * 70
        if (matchedTokenCount == context.queryTokens.count { it.length >= 2 }) {
            boost += 120
        }
        return boost
    }

    private fun isFuzzyTokenMatch(queryToken: String, candidateToken: String): Boolean {
        if (candidateToken.isBlank()) return false

        val maxDistance = when {
            queryToken.length <= 6 -> 1
            else -> 2
        }

        if (abs(queryToken.length - candidateToken.length) > maxDistance) {
            return false
        }

        return boundedLevenshteinDistance(queryToken, candidateToken, maxDistance) <= maxDistance
    }

    /**
     * Levenshtein distance with early exit when cost exceeds [maxDistance].
     */
    private fun boundedLevenshteinDistance(a: String, b: String, maxDistance: Int): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val previous = IntArray(b.length + 1) { it }
        val current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var minInRow = current[0]
            val aChar = a[i - 1]

            for (j in 1..b.length) {
                val substitutionCost = if (aChar == b[j - 1]) 0 else 1
                val insert = current[j - 1] + 1
                val delete = previous[j] + 1
                val replace = previous[j - 1] + substitutionCost
                current[j] = minOf(insert, delete, replace)
                if (current[j] < minInRow) {
                    minInRow = current[j]
                }
            }

            if (minInRow > maxDistance) {
                return maxDistance + 1
            }

            for (j in previous.indices) {
                previous[j] = current[j]
            }
        }

        return previous[b.length]
    }
}
