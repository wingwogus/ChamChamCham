package com.chamchamcham.domain.coaching.reportfeedback

import com.chamchamcham.domain.common.BaseTimeEntity
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farming.FarmingRecord
import com.chamchamcham.domain.farming.WorkType
import com.chamchamcham.domain.member.Member
import com.chamchamcham.domain.report.CycleReportStatistics
import com.chamchamcham.domain.report.FarmingCycleReport
import com.chamchamcham.domain.report.FarmingCycleReportProjection
import com.chamchamcham.domain.report.FarmingCycleReportStatus
import com.chamchamcham.domain.report.FarmingCycleStartBasis
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaType
import org.hibernate.type.format.FormatMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:report-feedback-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.jpa.properties.hibernate.type.json_format_mapper=com.chamchamcham.domain.coaching.reportfeedback.TestReportFeedbackJsonFormatMapper",
    ],
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportFeedbackRepositoryTest @Autowired constructor(
    private val entityManager: TestEntityManager,
    private val repository: ReportFeedbackRepository,
) {
    private val completedAt = LocalDateTime.of(2026, 7, 1, 9, 0)
    private lateinit var member: Member
    private lateinit var report: FarmingCycleReport

    @BeforeEach
    fun setUp() {
        member = persist(Member(email = "member@example.com", passwordHash = null))
        val farm = persist(Farm(owner = member, name = "약초농장", roadAddress = "강원도 평창군"))
        val crop = persist(
            Crop(externalNo = 422, name = "황기", usePartCategory = CropUsePartCategory.ROOT_BARK),
        )
        val finalHarvest = persist(
            FarmingRecord(
                member = member,
                farm = farm,
                crop = crop,
                workType = WorkType.HARVEST,
                workedAt = completedAt,
                weatherCondition = "맑음",
                weatherTemperature = 24,
                memo = "최종 수확",
                entryMode = "MANUAL",
            ),
        )
        report = persist(
            FarmingCycleReport.create(
                member = member,
                farm = farm,
                crop = crop,
                projection = FarmingCycleReportProjection(
                    status = FarmingCycleReportStatus.COMPLETED,
                    startsAt = completedAt.minusMonths(6),
                    endsAt = completedAt,
                    startBasis = FarmingCycleStartBasis.FIRST_RECORD,
                    finalHarvestRecord = finalHarvest,
                    statisticsSchemaVersion = 1,
                    statistics = CycleReportStatistics.empty(),
                ),
            ),
        )
    }

    @Test
    fun `different work types coexist for the same report`() {
        repository.saveAll(
            listOf(
                pendingFeedback(WorkType.WATERING),
                pendingFeedback(WorkType.HARVEST),
            ),
        )
        repository.flush()
        entityManager.clear()

        val feedbacks = repository.findAllByReport_IdAndMember_Id(
            requireNotNull(report.id),
            requireNotNull(member.id),
        )

        assertThat(feedbacks.map { it.workType })
            .containsExactlyInAnyOrder(WorkType.WATERING, WorkType.HARVEST)
    }

    @Test
    fun `the same work type cannot be duplicated for one report`() {
        repository.saveAndFlush(pendingFeedback(WorkType.WATERING))
        repository.save(pendingFeedback(WorkType.WATERING))

        assertThatThrownBy { repository.flush() }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun <T : BaseTimeEntity> persist(entity: T): T {
        setTimestamps(entity)
        return entityManager.persistAndFlush(entity)
    }

    private fun pendingFeedback(workType: WorkType): ReportFeedback =
        ReportFeedback.pending(member, report, workType).also(::setTimestamps)

    private fun setTimestamps(entity: BaseTimeEntity) {
        BaseTimeEntity::class.java.getDeclaredField("createdAt").apply {
            isAccessible = true
            set(entity, completedAt)
        }
        BaseTimeEntity::class.java.getDeclaredField("updatedAt").apply {
            isAccessible = true
            set(entity, completedAt)
        }
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackages = ["com.chamchamcham.domain"])
@EnableJpaRepositories(basePackages = ["com.chamchamcham.domain"])
private class ReportFeedbackRepositoryTestApplication

class TestReportFeedbackJsonFormatMapper : FormatMapper {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> fromString(
        charSequence: CharSequence,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions,
    ): T = when {
        javaType.javaTypeClass == CycleReportStatistics::class.java -> CycleReportStatistics.empty() as T
        Map::class.java.isAssignableFrom(javaType.javaTypeClass) -> emptyMap<String, Any?>() as T
        List::class.java.isAssignableFrom(javaType.javaTypeClass) -> emptyList<Any?>() as T
        else -> javaType.fromString(charSequence)
    }

    override fun <T : Any?> toString(
        value: T,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions,
    ): String = when (value) {
        is CycleReportStatistics, is Map<*, *> -> "{}"
        is List<*> -> "[]"
        else -> javaType.toString(value)
    }
}
