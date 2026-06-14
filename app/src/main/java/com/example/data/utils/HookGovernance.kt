package com.example.data.utils

import com.example.data.state.HookRecord
import com.example.data.state.RuntimeStateDelta

/**
 * HookGovernance - Hook governance utilities.
 *
 * This is the Kotlin Android equivalent of the TypeScript HookGovernance module.
 * It handles:
 * - Collecting stale hook debt
 * - Evaluating hook admission
 * - Classifying hook disposition
 */

enum class HookDisposition {
    NONE, MENTION, ADVANCE, RESOLVE, DEFER
}

data class HookAdmissionCandidate(
    val type: String,
    val expectedPayoff: String? = null,
    val payoffTiming: String? = null,
    val notes: String? = null
)

data class HookAdmissionDecision(
    val admit: Boolean,
    val reason: String, // "admit", "missing_type", "missing_payoff_signal", "duplicate_family"
    val matchedHookId: String? = null
)

/**
 * HookGovernance - Main class for hook governance.
 */
class HookGovernance {

    companion object {
        private val STOP_WORDS = setOf(
            "that", "this", "with", "from", "into", "still", "just",
            "have", "will", "reveal"
        )

        /**
         * Collect stale hook debt.
         */
        fun collectStaleHookDebt(
            hooks: List<HookRecord>,
            chapterNumber: Int,
            targetChapters: Int? = null,
            staleAfterChapters: Int? = null
        ): List<HookRecord> {
            return hooks
                .filter { it.status != "resolved" && it.status != "deferred" }
                .filter { it.startChapter <= chapterNumber }
                .filter { hook ->
                    if (staleAfterChapters != null) {
                        hook.lastAdvancedChapter <= chapterNumber - staleAfterChapters
                    } else {
                        // Simple stale detection: not advanced in last 5 chapters
                        hook.lastAdvancedChapter <= chapterNumber - 5
                    }
                }
                .sortedWith(compareBy<HookRecord> { it.lastAdvancedChapter }
                    .thenBy { it.startChapter }
                    .thenBy { it.hookId })
        }

        /**
         * Evaluate hook admission.
         */
        fun evaluateHookAdmission(
            candidate: HookAdmissionCandidate,
            activeHooks: List<HookRecord>
        ): HookAdmissionDecision {
            val candidateType = normalizeText(candidate.type)
            if (candidateType.isEmpty()) {
                return HookAdmissionDecision(
                    admit = false,
                    reason = "missing_type"
                )
            }

            val payoffSignal = listOfNotNull(candidate.expectedPayoff, candidate.notes)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim()

            if (payoffSignal.isEmpty()) {
                return HookAdmissionDecision(
                    admit = false,
                    reason = "missing_payoff_signal"
                )
            }

            val candidateNormalized = normalizeText(
                listOf(
                    candidate.type,
                    candidate.expectedPayoff ?: "",
                    candidate.payoffTiming ?: "",
                    candidate.notes ?: ""
                ).joinToString(" ")
            )
            val candidateTerms = extractTerms(candidateNormalized)
            val candidateChineseBigrams = extractChineseBigrams(candidateNormalized)

            for (hook in activeHooks) {
                val activeNormalized = normalizeText(
                    listOf(
                        hook.type,
                        hook.expectedPayoff,
                        hook.payoffTiming ?: "",
                        hook.notes
                    ).joinToString(" ")
                )

                if (candidateNormalized == activeNormalized) {
                    return HookAdmissionDecision(
                        admit = false,
                        reason = "duplicate_family",
                        matchedHookId = hook.hookId
                    )
                }

                if (candidateType != normalizeText(hook.type)) {
                    continue
                }

                val activeTerms = extractTerms(activeNormalized)
                val overlap = candidateTerms.intersect(activeTerms)
                val activeChineseBigrams = extractChineseBigrams(activeNormalized)
                val chineseOverlap = candidateChineseBigrams.intersect(activeChineseBigrams)

                if (overlap.size >= 2 || chineseOverlap.size >= 3) {
                    return HookAdmissionDecision(
                        admit = false,
                        reason = "duplicate_family",
                        matchedHookId = hook.hookId
                    )
                }
            }

            return HookAdmissionDecision(
                admit = true,
                reason = "admit"
            )
        }

        /**
         * Classify hook disposition.
         */
        fun classifyHookDisposition(
            hookId: String,
            delta: RuntimeStateDelta
        ): HookDisposition {
            if (hookId in delta.hookOps.defer) {
                return HookDisposition.DEFER
            }

            if (hookId in delta.hookOps.resolve) {
                return HookDisposition.RESOLVE
            }

            if (delta.hookOps.upsert.any { it.hookId == hookId && it.lastAdvancedChapter == delta.chapter }) {
                return HookDisposition.ADVANCE
            }

            if (hookId in delta.hookOps.mention) {
                return HookDisposition.MENTION
            }

            return HookDisposition.NONE
        }

        private fun normalizeText(value: String): String {
            return value
                .trim()
                .lowercase()
                .replace(Regex("[^a-z0-9\u4e00-\u9fff]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        private fun extractTerms(value: String): Set<String> {
            val english = value
                .split(" ")
                .map { it.trim() }
                .filter { it.length >= 4 }
                .filter { it !in STOP_WORDS }
            val chinese = Regex("[\u4e00-\u9fff]{2,6}").findAll(value).map { it.value }.toList()
            return (english + chinese).toSet()
        }

        private fun extractChineseBigrams(value: String): Set<String> {
            val segments = Regex("[\u4e00-\u9fff]+").findAll(value).map { it.value }.toList()
            val terms = mutableSetOf<String>()

            for (segment in segments) {
                if (segment.length < 2) {
                    continue
                }

                for (index in 0..segment.length - 2) {
                    terms.add(segment.substring(index, index + 2))
                }
            }

            return terms
        }
    }
}

/**
 * Extension function to collect stale hook debt.
 */
fun List<HookRecord>.collectStaleHookDebt(
    chapterNumber: Int,
    targetChapters: Int? = null,
    staleAfterChapters: Int? = null
): List<HookRecord> {
    return HookGovernance.collectStaleHookDebt(this, chapterNumber, targetChapters, staleAfterChapters)
}

/**
 * Extension function to evaluate hook admission.
 */
fun HookAdmissionCandidate.evaluateAdmission(activeHooks: List<HookRecord>): HookAdmissionDecision {
    return HookGovernance.evaluateHookAdmission(this, activeHooks)
}

/**
 * Extension function to classify hook disposition.
 */
fun RuntimeStateDelta.classifyHookDisposition(hookId: String): HookDisposition {
    return HookGovernance.classifyHookDisposition(hookId, this)
}
