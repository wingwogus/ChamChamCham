package com.chamchamcham.domain.farming

import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.member.Member
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class FarmingRecordTest {
    private val member = Member(id = UUID.randomUUID(), email = "member@example.com", passwordHash = null)
    private val farm = Farm(id = UUID.randomUUID(), owner = member, name = "약초농장", roadAddress = "강원도 평창군")
    private val crop = Crop(
        id = UUID.randomUUID(),
        externalNo = 422,
        name = "황기",
        usePartCategory = CropUsePartCategory.ROOT_BARK,
    )

    @Test
    fun `record update and delete advance feedback source revision`() {
        val record = farmingRecord(sourceRevision = 1)

        record.update(record.farm, record.crop, WorkType.WATERING, record.workedAt, "맑음", 24, "수정된 기록 메모 30자 이상")
        assertThat(record.sourceRevision).isEqualTo(2)

        record.softDelete()
        assertThat(record.sourceRevision).isEqualTo(3)
    }

    private fun farmingRecord(sourceRevision: Long): FarmingRecord {
        return FarmingRecord(
            id = UUID.randomUUID(),
            member = member,
            farm = farm,
            crop = crop,
            workType = WorkType.HARVEST,
            workedAt = LocalDateTime.of(2026, 6, 15, 8, 30),
            weatherCondition = "맑음",
            weatherTemperature = 24,
            memo = "최종 수확",
            entryMode = "MANUAL",
            sourceRevision = sourceRevision,
        )
    }
}
