package com.chamchamcham.api.farm.dto

import com.chamchamcham.application.weather.FarmWeatherResult
import java.time.LocalDateTime

object WeatherResponses {
    data class CurrentWeatherResponse(
        val temperature: Int,
        val weatherCondition: String,
        val observedAt: LocalDateTime,
        val address: String
    ) {
        companion object {
            fun from(result: FarmWeatherResult.CurrentDetail): CurrentWeatherResponse =
                CurrentWeatherResponse(
                    temperature = result.snapshot.temperature,
                    weatherCondition = result.snapshot.skyCondition,
                    observedAt = result.snapshot.observedAt,
                    address = result.roadAddress
                )
        }
    }
}
