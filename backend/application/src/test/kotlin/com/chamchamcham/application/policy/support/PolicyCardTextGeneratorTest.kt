package com.chamchamcham.application.policy.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PolicyCardTextGeneratorTest {
    private val generator = PolicyCardTextGenerator()

    @Test
    fun `eligibility summary is deterministic and within nineteen characters`() {
        val summary = generator.eligibilitySummary("농업경영정보를 등록한 농업·임업인·법인으로 친환경인증을 받은 자")

        assertThat(summary).isEqualTo("친환경 인증 농업인")
        assertThat(summary.length).isLessThanOrEqualTo(19)
    }

    @Test
    fun `benefit summary uses fallback for blank source text`() {
        val summary = generator.benefitSummary("   ")

        assertThat(summary).isEqualTo("상세 지원 확인")
        assertThat(summary.length).isLessThanOrEqualTo(19)
    }

    @Test
    fun `period label is nineteen characters or fewer`() {
        val label = generator.periodLabel(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 6, 30), null)

        assertThat(label).isEqualTo("2026.03.25~06.30")
        assertThat(label.length).isLessThanOrEqualTo(19)
    }

    @Test
    fun `period label preserves agency inquiry notice`() {
        val label = generator.periodLabel(null, null, "접수기관문의")

        assertThat(label).isEqualTo("접수기관문의")
    }
}
