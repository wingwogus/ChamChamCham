package com.chamchamcham.application.coaching.rag.chat

import com.chamchamcham.application.coaching.rag.common.CoachingCitationRef
import com.chamchamcham.application.coaching.rag.common.CoachingStructuredResult
import com.chamchamcham.application.coaching.rag.common.RagAuditResult
import com.chamchamcham.application.coaching.rag.common.RagModelInfo
import java.util.UUID

data class CoachingRagResult(
    val result: CoachingStructuredResult,
    val audit: RagAuditResult,
    val model: RagModelInfo,
    val savedFeedbackId: UUID? = null
) {
    val answer: String
        get() = result.summary

    val citations: List<CoachingCitationRef>
        get() = result.citations
}
