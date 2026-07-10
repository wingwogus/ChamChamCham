package com.chamchamcham.domain.coaching

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CoachingFeedbackRepository : JpaRepository<CoachingFeedback, UUID> {
    fun findByFeedbackTypeAndRecord_IdAndSourceRevision(
        feedbackType: FeedbackType,
        recordId: UUID,
        sourceRevision: Long,
    ): CoachingFeedback?

    fun findAllByFeedbackTypeAndRecord_IdAndStatusIn(
        feedbackType: FeedbackType,
        recordId: UUID,
        statuses: Collection<CoachingFeedbackStatus>,
    ): List<CoachingFeedback>

    fun findTopByFeedbackTypeAndRecord_IdAndStatusOrderByUpdatedAtDesc(
        feedbackType: FeedbackType,
        recordId: UUID,
        status: CoachingFeedbackStatus,
    ): CoachingFeedback?

    fun findByIdAndMember_Id(id: UUID, memberId: UUID): CoachingFeedback?
}
