package com.chamchamcham.application.farm

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FarmInputValidator {
    fun validate(draft: FarmCommand.Draft, cropIds: List<UUID>) {
        if (
            draft.name.isBlank() ||
            draft.roadAddress.isBlank() ||
            !isLatitude(draft.latitude) ||
            !isLongitude(draft.longitude) ||
            draft.areaSqm?.signum() == -1 ||
            draft.areaSqm?.signum() == 0 ||
            cropIds.size !in MIN_CROP_COUNT..MAX_CROP_COUNT ||
            cropIds.size != cropIds.toSet().size ||
            draft.boundaryCoordinates.any { !isLatitude(it.latitude) || !isLongitude(it.longitude) }
        ) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun isLatitude(value: Double): Boolean = value.isFinite() && value in MIN_LATITUDE..MAX_LATITUDE

    private fun isLongitude(value: Double): Boolean = value.isFinite() && value in MIN_LONGITUDE..MAX_LONGITUDE

    private companion object {
        const val MIN_CROP_COUNT = 1
        const val MAX_CROP_COUNT = 5
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}
