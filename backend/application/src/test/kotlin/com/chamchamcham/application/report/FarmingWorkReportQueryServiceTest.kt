package com.chamchamcham.application.report

import com.chamchamcham.application.common.OpaqueCursorCodec
import com.chamchamcham.domain.common.BaseTimeEntity
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farming.EntryMode
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.FarmingWorkReportSourceRepository
import com.chamchamcham.domain.farming.FarmingWorkReportSourceSnapshot
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.member.Member
import com.chamchamcham.domain.report.FarmingCycleReportQueryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FarmingWorkReportQueryServiceTest {
    private val memberId = id("001")
    private val farmId = id("101")
    private val cropId = id("201")
    private val reportId = id("301")
    private val finalHarvestId = id("406")
    private val baseTime = LocalDateTime.of(2026, 1, 1, 9, 0)
    private val cursorCodec = OpaqueCursorCodec()

    private val member = Member(id = memberId, email = "member@example.com", passwordHash = null)
    private val farm = Farm(id = farmId, owner = member, name = "약초농장", roadAddress = "서울시 강남구")
    private val crop = Crop(
        id = cropId,
        externalNo = 422,
        name = "황기",
        usePartCategory = CropUsePartCategory.ROOT_BARK,
    )

    @Mock private lateinit var queryRepository: FarmingCycleReportQueryRepository
    @Mock private lateinit var sourceRepository: FarmingWorkReportSourceRepository

    private lateinit var service: FarmingWorkReportQueryService

    @BeforeEach
    fun setUp() {
        service = FarmingWorkReportQueryService(
            queryRepository = queryRepository,
            sourceRepository = sourceRepository,
            partitioner = FarmingCyclePartitioner(),
            cursorCodec = cursorCodec,
        )
    }

    @Test
    fun `list uses lookahead and exact cycle thumbnails with latest pictured fallback`() {
        val expectedCondition = FarmingCycleReportQueryRepository.WorkItemSearchCondition(
            memberId = memberId,
            farmId = farmId,
            cropId = cropId,
            workType = null,
            cursor = null,
            size = 3,
        )
        val watering = workItem(WorkType.WATERING, recordCount = 2)
        val harvest = workItem(WorkType.HARVEST, recordCount = 1)
        val lookahead = workItem(WorkType.ETC, recordCount = 1)
        `when`(queryRepository.searchCompletedWorkItems(expectedCondition)).thenReturn(
            FarmingCycleReportQueryRepository.WorkItemSearchResult(listOf(watering, harvest, lookahead)),
        )

        val previousWatering = record("401", WorkType.WATERING, day = 1)
        val previousFinal = record("402", WorkType.HARVEST, day = 2)
        val picturedWatering = record("403", WorkType.WATERING, day = 3, createdMinute = 1)
        val latestWateringWithoutPhoto = record("404", WorkType.WATERING, day = 3, createdMinute = 2)
        val otherWorkType = record("405", WorkType.PRUNING, day = 4)
        val targetFinal = record("406", WorkType.HARVEST, day = 5)
        val adjacentWatering = record("407", WorkType.WATERING, day = 6)
        val adjacentFinal = record("408", WorkType.HARVEST, day = 7)
        `when`(sourceRepository.load(memberId, setOf(farmId), setOf(cropId))).thenReturn(
            FarmingWorkReportSourceSnapshot(
                records = listOf(
                    adjacentFinal,
                    latestWateringWithoutPhoto,
                    targetFinal,
                    previousFinal,
                    adjacentWatering,
                    picturedWatering,
                    previousWatering,
                    otherWorkType,
                ),
                finalHarvestRecordIds = setOf(
                    requireNotNull(previousFinal.id),
                    requireNotNull(targetFinal.id),
                    requireNotNull(adjacentFinal.id),
                ),
                firstImageUrlByRecordId = mapOf(
                    requireNotNull(previousWatering.id) to "https://img/previous-cycle.jpg",
                    requireNotNull(picturedWatering.id) to "https://img/watering.jpg",
                    requireNotNull(otherWorkType.id) to "https://img/other-work-type.jpg",
                    requireNotNull(targetFinal.id) to "https://img/harvest.jpg",
                    requireNotNull(adjacentWatering.id) to "https://img/adjacent-cycle.jpg",
                ),
            ),
        )

        val page = service.list(
            FarmingWorkReportSearchCondition(
                memberId = memberId,
                farmId = farmId,
                cropId = cropId,
                workType = null,
                cursor = null,
                size = 2,
            ),
        )

        assertThat(page.items.map { it.workType }).containsExactly(WorkType.WATERING, WorkType.HARVEST)
        assertThat(page.items.first().thumbnailUrl).isEqualTo("https://img/watering.jpg")
        assertThat(page.items.last().thumbnailUrl).isEqualTo("https://img/harvest.jpg")
        val decoded = cursorCodec.decode(
            requireNotNull(page.nextCursor),
            FarmingWorkReportCursorPayload::class.java,
        )
        assertThat(decoded.reportId).isEqualTo(page.items.last().reportId)
        assertThat(decoded.workType).isEqualTo(page.items.last().workType)
        verify(queryRepository).searchCompletedWorkItems(expectedCondition)
        verify(sourceRepository).load(memberId, setOf(farmId), setOf(cropId))
    }

    @Test
    fun `list decodes item cursor and returns null thumbnail without a pictured matching record`() {
        val cursorPayload = FarmingWorkReportCursorPayload(
            endsAt = day(30),
            reportId = id("399"),
            workType = WorkType.WATERING,
        )
        val cursor = cursorCodec.encode(cursorPayload)
        val expectedCondition = FarmingCycleReportQueryRepository.WorkItemSearchCondition(
            memberId = memberId,
            farmId = null,
            cropId = null,
            workType = WorkType.PEST_CONTROL,
            cursor = FarmingCycleReportQueryRepository.WorkItemCursor(
                endsAt = cursorPayload.endsAt,
                reportId = cursorPayload.reportId,
                workType = cursorPayload.workType,
            ),
            size = 2,
        )
        val pestControl = workItem(WorkType.PEST_CONTROL, recordCount = 1)
        `when`(queryRepository.searchCompletedWorkItems(expectedCondition)).thenReturn(
            FarmingCycleReportQueryRepository.WorkItemSearchResult(listOf(pestControl)),
        )
        val matchingRecord = record("405", WorkType.PEST_CONTROL, day = 4)
        val targetFinal = record("406", WorkType.HARVEST, day = 5)
        `when`(sourceRepository.load(memberId, setOf(farmId), setOf(cropId))).thenReturn(
            FarmingWorkReportSourceSnapshot(
                records = listOf(matchingRecord, targetFinal),
                finalHarvestRecordIds = setOf(requireNotNull(targetFinal.id)),
                firstImageUrlByRecordId = emptyMap(),
            ),
        )

        val page = service.list(
            FarmingWorkReportSearchCondition(
                memberId = memberId,
                farmId = null,
                cropId = null,
                workType = WorkType.PEST_CONTROL,
                cursor = cursor,
                size = 1,
            ),
        )

        assertThat(page.items.single().thumbnailUrl).isNull()
        assertThat(page.nextCursor).isNull()
        verify(queryRepository).searchCompletedWorkItems(expectedCondition)
        verify(sourceRepository).load(memberId, setOf(farmId), setOf(cropId))
    }

    private fun workItem(
        workType: WorkType,
        recordCount: Int,
    ): FarmingCycleReportQueryRepository.WorkItem =
        FarmingCycleReportQueryRepository.WorkItem(
            reportId = reportId,
            farmId = farmId,
            farmName = farm.name,
            cropId = cropId,
            cropName = crop.name,
            startsAt = day(1),
            endsAt = day(5),
            finalHarvestRecordId = finalHarvestId,
            workType = workType,
            recordCount = recordCount,
            lastWorkedOn = LocalDate.of(2026, 1, 5),
        )

    private fun record(
        suffix: String,
        workType: WorkType,
        day: Long,
        createdMinute: Long = 0,
    ): FarmingRecord =
        FarmingRecord(
            id = id(suffix),
            member = member,
            farm = farm,
            crop = crop,
            workType = workType,
            workedAt = day(day),
            weatherCondition = "맑음",
            weatherTemperature = 20,
            memo = "memo",
            entryMode = EntryMode.MANUAL,
        ).also { entity -> setCreatedAt(entity, day(day).plusMinutes(createdMinute)) }

    private fun setCreatedAt(entity: BaseTimeEntity, createdAt: LocalDateTime) {
        BaseTimeEntity::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(entity, createdAt)
        }
    }

    private fun day(day: Long): LocalDateTime = baseTime.plusDays(day)

    private fun id(suffix: String): UUID =
        UUID.fromString("00000000-0000-0000-0000-000000000$suffix")
}
