package com.chamchamcham.api.coaching.dto

import com.chamchamcham.application.coaching.rag.record.TodayRecordFeedbackResult

object TodayRecordFeedbackResponses {
    data class FeedbackResponse(
        val result: CoachingRagResponses.StructuredResultResponse,
        val audit: CoachingRagResponses.AuditResponse,
        val model: CoachingRagResponses.ModelResponse,
        val contextWarnings: List<String>
    ) {
        companion object {
            fun from(result: TodayRecordFeedbackResult): FeedbackResponse {
                return FeedbackResponse(
                    result = CoachingRagResponses.StructuredResultResponse.from(result.result),
                    audit = CoachingRagResponses.AuditResponse(result.audit.status, result.audit.warnings),
                    model = CoachingRagResponses.ModelResponse(result.model.embedding, result.model.chat),
                    contextWarnings = result.contextWarnings
                )
            }
        }
    }
}
