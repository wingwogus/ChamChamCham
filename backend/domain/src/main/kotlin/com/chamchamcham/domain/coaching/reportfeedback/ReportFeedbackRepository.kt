package com.chamchamcham.domain.coaching.reportfeedback

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ReportFeedbackRepository : JpaRepository<ReportFeedback, UUID> {
    fun existsByReport_Id(reportId: UUID): Boolean

    fun findAllByReport_IdAndMember_Id(reportId: UUID, memberId: UUID): List<ReportFeedback>

    fun findByIdAndMember_Id(id: UUID, memberId: UUID): ReportFeedback?

    @Deprecated("Report feedback is now a report/work-type collection")
    fun findByReport_Id(reportId: UUID): ReportFeedback?

    @Deprecated("Report feedback is now a report/work-type collection")
    fun findByReport_IdAndMember_Id(reportId: UUID, memberId: UUID): ReportFeedback?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select feedback from ReportFeedback feedback where feedback.id = :id and feedback.member.id = :memberId")
    fun findByIdAndMemberIdForUpdate(
        @Param("id") id: UUID,
        @Param("memberId") memberId: UUID,
    ): ReportFeedback?
}
