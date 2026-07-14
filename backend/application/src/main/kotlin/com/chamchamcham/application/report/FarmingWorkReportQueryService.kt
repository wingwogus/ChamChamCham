package com.chamchamcham.application.report

import com.chamchamcham.application.common.OpaqueCursorCodec
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.FarmingWorkReportSourceRepository
import com.chamchamcham.domain.farming.FarmingWorkReportSourceSnapshot
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FarmingWorkReportQueryService(
    private val queryRepository: FarmingCycleReportQueryRepository,
    private val sourceRepository: FarmingWorkReportSourceRepository,
    private val partitioner: FarmingCyclePartitioner,
    private val cursorCodec: OpaqueCursorCodec,
) {
    fun list(condition: FarmingWorkReportSearchCondition): FarmingWorkReportResult.Page {
        val result = queryRepository.searchCompletedWorkItems(
            FarmingCycleReportQueryRepository.WorkItemSearchCondition(
                memberId = condition.memberId,
                farmId = condition.farmId,
                cropId = condition.cropId,
                workType = condition.workType,
                cursor = decodeCursor(condition.cursor),
                size = condition.size + 1,
            ),
        )
        val visibleRows = result.rows.take(condition.size)
        val snapshot = sourceRepository.load(
            memberId = condition.memberId,
            farmIds = visibleRows.mapTo(mutableSetOf(), FarmingCycleReportQueryRepository.WorkItem::farmId),
            cropIds = visibleRows.mapTo(mutableSetOf(), FarmingCycleReportQueryRepository.WorkItem::cropId),
        )
        val slicesByScope = buildSlicesByScope(visibleRows, snapshot)

        return FarmingWorkReportResult.Page(
            items = visibleRows.map { row ->
                row.toResult(
                    thumbnailUrl = findThumbnailUrl(row, slicesByScope[row.scopeKey()].orEmpty(), snapshot),
                )
            },
            nextCursor = if (result.rows.size > condition.size) {
                visibleRows.lastOrNull()?.let(::encodeCursor)
            } else {
                null
            },
        )
    }

    private fun buildSlicesByScope(
        rows: List<FarmingCycleReportQueryRepository.WorkItem>,
        snapshot: FarmingWorkReportSourceSnapshot,
    ): Map<WorkScope, List<CycleSlice>> =
        rows.map { row -> row.scopeKey() }
            .distinct()
            .associateWith { scope ->
                partitioner.partition(
                    snapshot.records
                        .asSequence()
                        .filter { record -> record.matches(scope) }
                        .map { record -> record.toCycleSource(snapshot) }
                        .toList(),
                )
            }

    private fun findThumbnailUrl(
        row: FarmingCycleReportQueryRepository.WorkItem,
        slices: List<CycleSlice>,
        snapshot: FarmingWorkReportSourceSnapshot,
    ): String? {
        val cycle = slices.singleOrNull { slice ->
            slice.finalHarvestRecordId == row.finalHarvestRecordId
        } ?: return null
        val picturedRecord = cycle.records
            .asSequence()
            .filter { record -> record.workType == row.workType }
            .filter { record -> snapshot.firstImageUrlByRecordId.containsKey(record.id) }
            .maxWithOrNull(
                compareBy(
                    CycleReportSourceRecord::workedAt,
                    CycleReportSourceRecord::createdAt,
                    CycleReportSourceRecord::id,
                ),
            )
            ?: return null
        return snapshot.firstImageUrlByRecordId[picturedRecord.id]
    }

    private fun decodeCursor(cursor: String?): FarmingCycleReportQueryRepository.WorkItemCursor? {
        if (cursor.isNullOrBlank()) return null
        val payload = cursorCodec.decode(cursor, FarmingWorkReportCursorPayload::class.java)
        return FarmingCycleReportQueryRepository.WorkItemCursor(
            endsAt = payload.endsAt,
            reportId = payload.reportId,
            workType = payload.workType,
        )
    }

    private fun encodeCursor(row: FarmingCycleReportQueryRepository.WorkItem): String =
        cursorCodec.encode(
            FarmingWorkReportCursorPayload(
                endsAt = row.endsAt,
                reportId = row.reportId,
                workType = row.workType,
            ),
        )

    private fun FarmingCycleReportQueryRepository.WorkItem.toResult(
        thumbnailUrl: String?,
    ): FarmingWorkReportResult.Item =
        FarmingWorkReportResult.Item(
            reportId = reportId,
            farmId = farmId,
            farmName = farmName,
            cropId = cropId,
            cropName = cropName,
            startsAt = startsAt,
            endsAt = endsAt,
            finalHarvestRecordId = finalHarvestRecordId,
            workType = workType,
            recordCount = recordCount,
            lastWorkedOn = lastWorkedOn,
            thumbnailUrl = thumbnailUrl,
        )

    private fun FarmingRecord.toCycleSource(
        snapshot: FarmingWorkReportSourceSnapshot,
    ): CycleReportSourceRecord {
        val recordId = requireNotNull(id) { "Persisted farming record id is required" }
        return CycleReportSourceRecord(
            id = recordId,
            workedAt = workedAt,
            createdAt = createdAt,
            workType = workType,
            weatherCondition = weatherCondition,
            weatherTemperature = weatherTemperature,
            hasPhoto = snapshot.firstImageUrlByRecordId.containsKey(recordId),
            memo = memo,
            harvest = if (workType == WorkType.HARVEST) {
                HarvestReportSource(
                    amountKg = null,
                    medicinalPart = null,
                    growthPeriodMonths = null,
                    isLastHarvest = recordId in snapshot.finalHarvestRecordIds,
                )
            } else {
                null
            },
        )
    }

    private fun FarmingCycleReportQueryRepository.WorkItem.scopeKey(): WorkScope =
        WorkScope(farmId, cropId)

    private fun FarmingRecord.matches(scope: WorkScope): Boolean =
        farm.id == scope.farmId && crop.id == scope.cropId

    private data class WorkScope(
        val farmId: UUID,
        val cropId: UUID,
    )
}
