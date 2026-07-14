package com.chamchamcham.application.coaching.reportfeedback.generation

import com.chamchamcham.application.coaching.common.CoachingTextPolicy
import com.chamchamcham.domain.farming.WorkType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class ReportFeedbackPromptBuilderTest {
    private val recordId = UUID.randomUUID()
    private val previousReportId = UUID.randomUUID()

    @Test
    fun `prompt scopes instructions statistics and allowed evidence to one work type`() {
        val prompt = ReportFeedbackPromptBuilder().build(
            context = context(),
            evidence = listOf(
                ReportFeedbackEvidence(
                    id = "document-1",
                    title = "황기 관수 기술",
                    content = "관수 후 토양 수분을 확인한다.",
                ),
            ),
        )

        assertThat(prompt.system)
            .contains("대상 작업 타입 하나만")
            .contains("nextActions")
            .contains("빈 배열")
            .contains("선택한 작업의 다음 행동은 실행 방법이 드러나게 작성한다.")
            .contains("summary와 모든 text는 친근한 존댓말로 끝낸다.")
            .contains(CoachingTextPolicy.promptInstructions)
            .doesNotContain("다음 사이클 계획")
        assertThat(prompt.user)
            .contains("대상 작업: 물 주기")
            .contains("기록 횟수: 4회")
            .contains("평균 작업 간격: 3.5일")
            .contains("물을 준 방법: 호스로 조금씩 물을 줌")
            .contains("직전 기록 횟수: 3회")
            .contains("record:$recordId")
            .contains("report:$previousReportId")
            .contains("document-1")
            .contains("황기 관수 기술", "관수 후 토양 수분을 확인한다.")
            .doesNotContain(
                "WATERING",
                "recordCount",
                "averageIntervalDays",
                "details=",
                "DRIP",
                "LOW",
                "code=",
                "label=",
                "CategoryRef",
            )
            .doesNotContain("FERTILIZING")
            .doesNotContain("수확량")
    }

    @Test
    fun `unknown enum code is omitted instead of falling back to raw code or label`() {
        val context = context().copy(
            report = context().report.copy(
                statistics = mapOf(
                    "recordCount" to 1,
                    "methodDistribution" to listOf(
                        mapOf("code" to "NEW_METHOD", "label" to "새 방식", "count" to 1, "ratePct" to 100),
                    ),
                ),
            ),
            records = context().records.map {
                it.copy(
                    details = mapOf(
                        "watering" to mapOf(
                            "method" to mapOf("code" to "NEW_METHOD", "label" to "새 방식"),
                        ),
                    ),
                )
            },
        )

        val prompt = ReportFeedbackPromptBuilder().build(context, emptyList())

        assertThat(prompt.user).doesNotContain("NEW_METHOD", "새 방식")
    }

    private fun context() = ReportFeedbackContext(
        schemaVersion = REPORT_FEEDBACK_CONTEXT_SCHEMA_VERSION,
        workType = WorkType.WATERING,
        report = ReportFeedbackReport(
            id = UUID.randomUUID(),
            farmName = "약초농장",
            cropName = "황기",
            startsAt = LocalDateTime.of(2026, 3, 1, 9, 0),
            endsAt = LocalDateTime.of(2026, 7, 1, 9, 0),
            statistics = mapOf(
                "recordCount" to 4,
                "averageIntervalDays" to 3.5,
                "amountDistribution" to listOf(
                    mapOf("code" to "LOW", "label" to "적음", "count" to 2, "ratePct" to 50),
                ),
                "methodDistribution" to listOf(
                    mapOf("code" to "DRIP", "label" to "점적관수", "count" to 4, "ratePct" to 100),
                ),
            ),
        ),
        records = listOf(
            ReportFeedbackRecord(
                id = recordId,
                workedAt = LocalDateTime.of(2026, 4, 1, 9, 0),
                workType = WorkType.WATERING,
                memo = "점적관수를 했어요.",
                details = mapOf(
                    "weatherCondition" to "맑음",
                    "weatherTemperature" to 18,
                    "hasPhoto" to false,
                    "watering" to mapOf(
                        "amount" to mapOf("code" to "LOW", "label" to "적음"),
                        "method" to mapOf("code" to "DRIP", "label" to "점적관수"),
                    ),
                ),
            ),
        ),
        previousReport = ReportFeedbackPreviousReport(
            id = previousReportId,
            startsAt = LocalDateTime.of(2025, 3, 1, 9, 0),
            endsAt = LocalDateTime.of(2025, 7, 1, 9, 0),
            statistics = mapOf(
                "recordCount" to 3,
                "averageIntervalDays" to 4.0,
                "methodDistribution" to listOf(
                    mapOf("code" to "DRIP", "label" to "점적관수", "count" to 3, "ratePct" to 100),
                ),
            ),
        ),
        warnings = emptyList(),
    )
}
