package com.chamchamcham.application.report

import com.chamchamcham.domain.farming.WorkType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object FarmingWorkReportResult {
    data class Item(
        val reportId: UUID,
        val farmId: UUID,
        val farmName: String,
        val cropId: UUID,
        val cropName: String,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        val finalHarvestRecordId: UUID,
        val workType: WorkType,
        val recordCount: Int,
        val lastWorkedOn: LocalDate?,
        val thumbnailUrl: String?,
    )

    data class Page(
        val items: List<Item>,
        val nextCursor: String?,
    )
}
