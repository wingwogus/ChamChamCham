package com.chamchamcham.application.report

import com.chamchamcham.domain.farming.WorkType
import java.time.LocalDateTime
import java.util.UUID

data class FarmingWorkReportCursorPayload(
    val endsAt: LocalDateTime,
    val reportId: UUID,
    val workType: WorkType,
)
