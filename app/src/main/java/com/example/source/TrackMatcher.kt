package com.example.source

import kotlin.math.abs
import kotlin.math.max

data class MatchResult(
    val matchedTrack: UnifiedTrack,
    val confidence: Float,
    val isConfidentMatch: Boolean
)

object TrackMatcher {

    fun findBestMatch(
        targetTitle: String,
        targetArtist: String,
        targetDurationSec: Int = 0,
        candidates: List<UnifiedTrack>,
        minConfidenceThreshold: Float = 0.55f
    ): MatchResult? {
        if (candidates.isEmpty()) return null

        var bestCandidate: UnifiedTrack? = null
        var highestScore = 0f

        val cleanTargetTitle = cleanString(targetTitle)
        val cleanTargetArtist = cleanString(targetArtist)

        for (candidate in candidates) {
            val candidateTitle = cleanString(candidate.title)
            val candidateArtist = cleanString(candidate.artist)

            val titleScore = computeSimilarity(cleanTargetTitle, candidateTitle)
            val artistScore = computeSimilarity(cleanTargetArtist, candidateArtist)

            // Duration penalty if duration is known for both
            var durationScore = 1.0f
            if (targetDurationSec > 0 && candidate.durationSec > 0) {
                val diff = abs(targetDurationSec - candidate.durationSec)
                durationScore = when {
                    diff <= 4 -> 1.0f
                    diff <= 10 -> 0.9f
                    diff <= 25 -> 0.75f
                    diff <= 45 -> 0.5f
                    else -> 0.2f
                }
            }

            // Weighted composite score: 60% Title, 30% Artist, 10% Duration
            val totalScore = (titleScore * 0.6f) + (artistScore * 0.3f) + (durationScore * 0.1f)

            if (totalScore > highestScore) {
                highestScore = totalScore
                bestCandidate = candidate
            }
        }

        val best = bestCandidate ?: return null
        return MatchResult(
            matchedTrack = best.copy(matchConfidence = highestScore),
            confidence = highestScore,
            isConfidentMatch = highestScore >= minConfidenceThreshold
        )
    }

    private fun cleanString(input: String): String {
        return input.lowercase()
            .replace(Regex("\\[.*?\\]|\\(.*?\\)"), "") // remove [Official Video], (Remastered)
            .replace(Regex("(?i)\\b(official|video|audio|lyrics|hd|4k|remastered|version|feat|ft)\\b"), "")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun computeSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        if (s1.contains(s2) || s2.contains(s1)) return 0.85f

        val words1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = s2.split(" ").filter { it.isNotBlank() }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return 0.0f

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        val jaccard = intersection.toFloat() / union.toFloat()
        val levenshtein = 1.0f - (levenshteinDistance(s1, s2).toFloat() / max(s1.length, s2.length).toFloat())

        return (jaccard * 0.7f + levenshtein * 0.3f).coerceIn(0f, 1f)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
