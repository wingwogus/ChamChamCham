package com.chamchamcham.application.weather

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farm.FarmRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FarmWeatherService(
    private val farmRepository: FarmRepository,
    private val weatherProvider: WeatherProvider
) {
    fun getCurrentWeather(memberId: UUID, farmId: UUID): FarmWeatherResult.CurrentDetail {
        val farm = farmRepository.findByIdAndOwnerId(farmId, memberId)
            ?: throw BusinessException(ErrorCode.FARM_NOT_FOUND)

        val latitude = farm.latitude
        val longitude = farm.longitude
        if (latitude == null || longitude == null) {
            throw BusinessException(ErrorCode.WEATHER_LOCATION_REQUIRED)
        }

        val snapshot = weatherProvider.fetchCurrentWeather(latitude, longitude)
        val forecast = runCatching { weatherProvider.fetchForecastPanel(latitude, longitude) }.getOrNull()
        return FarmWeatherResult.CurrentDetail(
            snapshot = snapshot,
            roadAddress = farm.roadAddress,
            precipitationProbability = forecast?.precipitationProbability,
            forecast = forecast?.dailyForecasts ?: emptyList(),
            uvIndex = null
        )
    }

    fun getCurrentWeather(memberId: UUID): FarmWeatherResult.CurrentDetail =
        getCurrentWeather(memberId, resolveDefaultFarm(memberId).id!!)

    private fun resolveDefaultFarm(memberId: UUID): Farm =
        farmRepository.findFirstByOwnerIdOrderByCreatedAtAsc(memberId)
            ?: throw BusinessException(ErrorCode.FARM_NOT_FOUND)
}
