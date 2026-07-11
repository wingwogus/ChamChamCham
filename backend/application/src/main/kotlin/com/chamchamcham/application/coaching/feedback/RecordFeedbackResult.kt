package com.chamchamcham.application.coaching.feedback

import com.chamchamcham.application.coaching.rag.record.RecordFeedbackActionCategory
import com.chamchamcham.application.coaching.rag.record.RecordFeedbackActionDue
import com.chamchamcham.domain.coaching.CoachingFeedbackStatus
import java.time.LocalDateTime
import java.util.UUID

data class RecordFeedbackResult(
    val feedbackId: UUID,
    val recordId: UUID,
    val status: CoachingFeedbackStatus,
    val sourceRevision: Long,
    val inputPrepared: Boolean,
    val failureCode: String?,
    val feedback: RecordFeedbackUserResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class RecordFeedbackUserResponse(
    val goodPoint: RecordFeedbackUserGoodPoint,
    val nextActions: List<RecordFeedbackUserNextAction>,
)

data class RecordFeedbackUserGoodPoint(
    val text: String,
)

data class RecordFeedbackUserNextAction(
    val text: String,
    val due: RecordFeedbackActionDue,
    val category: RecordFeedbackActionCategory,
)
