package com.chamchamcham.domain.report

import com.chamchamcham.domain.farming.WorkType
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class FarmingCycleReportQueryRepositoryImpl(
    private val entityManager: EntityManager,
) : FarmingCycleReportQueryRepository {
    override fun searchCompleted(
        condition: FarmingCycleReportQueryRepository.SearchCondition,
    ): FarmingCycleReportQueryRepository.SearchResult {
        val where = completedScopeWhere(condition.farmId, condition.cropId)
        val params = completedScopeParams(condition.memberId, condition.farmId, condition.cropId)

        condition.cursor?.let { cursor ->
            where += cursorPredicate()
            params["cursorEndsAt"] = cursor.endsAt
            params["cursorFinalHarvestRecordId"] = cursor.finalHarvestRecordId
        }

        return FarmingCycleReportQueryRepository.SearchResult(
            rows = findCompleted(where, params, condition.size),
        )
    }

    override fun searchCompletedWorkItems(
        condition: FarmingCycleReportQueryRepository.WorkItemSearchCondition,
    ): FarmingCycleReportQueryRepository.WorkItemSearchResult {
        val where = completedScopeWhere(condition.farmId, condition.cropId)
        val params = completedScopeParams(condition.memberId, condition.farmId, condition.cropId)
        val query = entityManager.createQuery(
            """
            select r.id, r.farm.id, r.farm.name, r.crop.id, r.crop.name,
                   r.startsAt, r.endsAt, r.finalHarvestRecord.id, r.statistics
            from FarmingCycleReport r
            where ${where.joinToString(" and ")}
            order by r.endsAt desc, r.id desc
            """.trimIndent(),
            Array<Any>::class.java,
        )
        params.forEach(query::setParameter)

        val rows = query.resultList
            .map(::toCompletedWorkProjection)
            .flatMap { projection ->
                val workTypes = condition.workType?.let(::listOf) ?: WorkType.entries
                workTypes.mapNotNull { workType -> projection.toWorkItem(workType) }
            }
            .sortedWith(
                compareByDescending<FarmingCycleReportQueryRepository.WorkItem> { it.endsAt }
                    .thenByDescending { it.reportId }
                    .thenBy { it.workType.ordinal },
            )
            .filter { item -> condition.cursor?.let { item.isAfter(it) } ?: true }
            .take(condition.size)

        return FarmingCycleReportQueryRepository.WorkItemSearchResult(rows)
    }

    override fun findPreviousCompleted(
        memberId: UUID,
        farmId: UUID,
        cropId: UUID,
        endsAt: LocalDateTime,
        finalHarvestRecordId: UUID,
    ): FarmingCycleReport? {
        val where = completedScopeWhere(farmId, cropId)
        val params = completedScopeParams(memberId, farmId, cropId)
        where += cursorPredicate()
        params["cursorEndsAt"] = endsAt
        params["cursorFinalHarvestRecordId"] = finalHarvestRecordId

        return findCompleted(where, params, maxResults = 1).firstOrNull()
    }

    private fun findCompleted(
        where: List<String>,
        params: Map<String, Any>,
        maxResults: Int,
    ): List<FarmingCycleReport> {
        val query = entityManager.createQuery(
            """
            select r
            from FarmingCycleReport r
            join fetch r.farm
            join fetch r.crop
            where ${where.joinToString(" and ")}
            order by r.endsAt desc, r.finalHarvestRecord.id desc
            """.trimIndent(),
            FarmingCycleReport::class.java,
        )
        params.forEach(query::setParameter)
        query.maxResults = maxResults
        return query.resultList
    }

    private fun completedScopeWhere(
        farmId: UUID?,
        cropId: UUID?,
    ): MutableList<String> = mutableListOf(
        "r.member.id = :memberId",
        "r.status = :completed",
    ).apply {
        farmId?.let { add("r.farm.id = :farmId") }
        cropId?.let { add("r.crop.id = :cropId") }
    }

    private fun completedScopeParams(
        memberId: UUID,
        farmId: UUID?,
        cropId: UUID?,
    ): MutableMap<String, Any> = mutableMapOf<String, Any>(
        "memberId" to memberId,
        "completed" to FarmingCycleReportStatus.COMPLETED,
    ).apply {
        farmId?.let { put("farmId", it) }
        cropId?.let { put("cropId", it) }
    }

    private fun cursorPredicate(): String =
        """
        (
          r.endsAt < :cursorEndsAt
          or (
            r.endsAt = :cursorEndsAt
            and r.finalHarvestRecord.id < :cursorFinalHarvestRecordId
          )
        )
        """.trimIndent()

    private fun toCompletedWorkProjection(row: Array<Any>): CompletedWorkProjection =
        CompletedWorkProjection(
            reportId = row[0] as UUID,
            farmId = row[1] as UUID,
            farmName = row[2] as String,
            cropId = row[3] as UUID,
            cropName = row[4] as String,
            startsAt = row[5] as LocalDateTime,
            endsAt = row[6] as LocalDateTime,
            finalHarvestRecordId = row[7] as UUID,
            statistics = row[8] as CycleReportStatistics,
        )

    private data class CompletedWorkProjection(
        val reportId: UUID,
        val farmId: UUID,
        val farmName: String,
        val cropId: UUID,
        val cropName: String,
        val startsAt: LocalDateTime,
        val endsAt: LocalDateTime,
        val finalHarvestRecordId: UUID,
        val statistics: CycleReportStatistics,
    ) {
        fun toWorkItem(workType: WorkType): FarmingCycleReportQueryRepository.WorkItem? {
            val (recordCount, lastWorkedOn) = statistics.summary(workType)
            if (recordCount <= 0) return null
            return FarmingCycleReportQueryRepository.WorkItem(
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
            )
        }
    }

    private fun FarmingCycleReportQueryRepository.WorkItem.isAfter(
        cursor: FarmingCycleReportQueryRepository.WorkItemCursor,
    ): Boolean =
        endsAt < cursor.endsAt ||
            (endsAt == cursor.endsAt && reportId < cursor.reportId) ||
            (endsAt == cursor.endsAt && reportId == cursor.reportId && workType.ordinal > cursor.workType.ordinal)
}

private fun CycleReportStatistics.summary(workType: WorkType) = when (workType) {
    WorkType.PLANTING -> planting.recordCount to planting.lastWorkedOn
    WorkType.WATERING -> watering.recordCount to watering.lastWorkedOn
    WorkType.FERTILIZING -> fertilizing.recordCount to fertilizing.lastWorkedOn
    WorkType.PEST_CONTROL -> pestControl.recordCount to pestControl.lastWorkedOn
    WorkType.WEEDING -> weeding.recordCount to weeding.lastWorkedOn
    WorkType.PRUNING -> pruning.recordCount to pruning.lastWorkedOn
    WorkType.HARVEST -> harvest.recordCount to harvest.lastWorkedOn
    WorkType.ETC -> etc.recordCount to etc.lastWorkedOn
}
