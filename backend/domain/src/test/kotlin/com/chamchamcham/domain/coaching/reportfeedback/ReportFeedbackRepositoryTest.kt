package com.chamchamcham.domain.coaching.reportfeedback

import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReportFeedbackRepositoryTest {
    @Test
    fun `report feedback uniqueness is scoped by report and work type`() {
        val table = ReportFeedback::class.java.getAnnotation(Table::class.java)
        val constraint = table.uniqueConstraints.single {
            it.name == "uk_report_feedback_report_work_type"
        }

        assertThat(constraint.columnNames)
            .containsExactly("report_id", "work_type")
    }
}
