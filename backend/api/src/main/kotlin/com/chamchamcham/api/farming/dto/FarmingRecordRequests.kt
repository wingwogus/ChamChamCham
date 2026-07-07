package com.chamchamcham.api.farming.dto

import com.chamchamcham.domain.farming.FertilizerAmountUnit
import com.chamchamcham.domain.farming.FertilizingMethod
import com.chamchamcham.domain.farming.GrowthPeriodUnit
import com.chamchamcham.domain.farming.HarvestAmountUnit
import com.chamchamcham.domain.farming.HarvestSource
import com.chamchamcham.domain.farming.IrrigationAmount
import com.chamchamcham.domain.farming.IrrigationMethod
import com.chamchamcham.domain.farming.PesticideAmountUnit
import com.chamchamcham.domain.farming.SeedAmountUnit
import com.chamchamcham.domain.farming.SeedSource
import com.chamchamcham.domain.farming.SeedlingUnit
import com.chamchamcham.domain.farming.SprayAmountUnit
import com.chamchamcham.domain.farming.WeedingMethod
import com.chamchamcham.domain.farming.WorkType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

object FarmingRecordRequests {
    data class SaveRecordRequest(
        @field:NotNull(message = "농지를 선택해주세요")
        val farmId: UUID?,

        @field:NotNull(message = "작물을 선택해주세요")
        val cropId: UUID?,

        @field:NotNull(message = "작업 유형을 선택해주세요")
        val workType: WorkType?,

        @field:NotNull(message = "작업 일시를 입력해주세요")
        val workedAt: LocalDateTime?,

        @field:NotBlank(message = "날씨 상태를 입력해주세요")
        val weatherCondition: String,

        @field:NotNull(message = "기온을 입력해주세요")
        val weatherTemperature: Int?,

        val memo: String? = null,

        val planting: PlantingDetailRequest? = null,
        val watering: WateringDetailRequest? = null,
        val fertilizing: FertilizingDetailRequest? = null,
        val pestControl: PestControlDetailRequest? = null,
        val weeding: WeedingDetailRequest? = null,
        val harvest: HarvestDetailRequest? = null,

        @field:Size(max = 5, message = "사진은 최대 5장까지 첨부할 수 있습니다")
        val mediaIds: List<UUID> = emptyList()
    )

    data class PlantingDetailRequest(
        val seedAmount: BigDecimal? = null,
        val seedAmountUnit: SeedAmountUnit? = null,
        val seedlingCount: Int? = null,
        val seedlingUnit: SeedlingUnit? = null,
        val seedSource: SeedSource? = null,
        val seedPurchasePlace: String? = null,
    )

    data class WateringDetailRequest(
        val irrigationAmount: IrrigationAmount? = null,
        val irrigationMethod: IrrigationMethod? = null,
    )

    data class FertilizingDetailRequest(
        val materialName: String,
        val amount: BigDecimal,
        val amountUnit: FertilizerAmountUnit,
        val applicationMethod: FertilizingMethod? = null,
    )

    data class PestControlDetailRequest(
        val pesticideName: String,
        val pesticideAmount: BigDecimal,
        val pesticideAmountUnit: PesticideAmountUnit,
        val totalSprayAmount: BigDecimal,
        val totalSprayAmountUnit: SprayAmountUnit,
        val pestTarget: String? = null,
    )

    data class WeedingDetailRequest(
        val weedingMethod: WeedingMethod? = null,
    )

    data class HarvestDetailRequest(
        val harvestAmount: BigDecimal,
        val harvestAmountUnit: HarvestAmountUnit,
        val harvestSource: HarvestSource = HarvestSource.CULTIVATED,
        val growthPeriod: Int,
        val growthPeriodUnit: GrowthPeriodUnit,
    )
}
