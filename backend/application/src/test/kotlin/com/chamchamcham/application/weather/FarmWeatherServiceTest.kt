package com.chamchamcham.application.weather

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.farm.Farm
import com.chamchamcham.domain.farm.FarmRepository
import com.chamchamcham.domain.member.Member
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class FarmWeatherServiceTest {

    @Mock private lateinit var farmRepository: FarmRepository
    @Mock private lateinit var weatherProvider: WeatherProvider

    private lateinit var service: FarmWeatherService

    private val memberId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val farmId = UUID.fromString("00000000-0000-0000-0000-000000000201")
    private val member = Member(id = memberId, email = "member@example.com", passwordHash = null)

    @BeforeEach
    fun setUp() {
        service = FarmWeatherService(farmRepository, weatherProvider)
    }

    @Test
    fun `농지 좌표로 날씨 스냅샷을 조회한다`() {
        val farm = Farm(
            id = farmId,
            owner = member,
            name = "약초농장",
            roadAddress = "서울시 강남구",
            latitude = 37.5665,
            longitude = 126.9780
        )
        val snapshot = WeatherSnapshot(
            temperature = 14,
            skyCondition = "맑음",
            observedAt = LocalDateTime.of(2026, 7, 8, 10, 0)
        )
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm)
        `when`(weatherProvider.fetchCurrentWeather(37.5665, 126.9780)).thenReturn(snapshot)

        val result = service.getCurrentWeather(memberId, farmId)

        assertThat(result.snapshot).isEqualTo(snapshot)
        assertThat(result.roadAddress).isEqualTo("서울시 강남구")
    }

    @Test
    fun `농지가 없거나 소유자가 아니면 FARM_NOT_FOUND를 던진다`() {
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(null)

        assertThatThrownBy { service.getCurrentWeather(memberId, farmId) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FARM_NOT_FOUND)

        verifyNoInteractions(weatherProvider)
    }

    @Test
    fun `농지에 좌표가 없으면 WEATHER_LOCATION_REQUIRED를 던진다`() {
        val farm = Farm(
            id = farmId,
            owner = member,
            name = "약초농장",
            roadAddress = "서울시 강남구"
        )
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm)

        assertThatThrownBy { service.getCurrentWeather(memberId, farmId) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.WEATHER_LOCATION_REQUIRED)

        verifyNoInteractions(weatherProvider)
    }

    @Test
    fun `농지 ID 없이 조회하면 가장 먼저 등록한 농지의 날씨를 반환한다`() {
        val farm = Farm(
            id = farmId,
            owner = member,
            name = "약초농장",
            roadAddress = "서울시 강남구",
            latitude = 37.5665,
            longitude = 126.9780
        )
        val snapshot = WeatherSnapshot(
            temperature = 14,
            skyCondition = "맑음",
            observedAt = LocalDateTime.of(2026, 7, 8, 10, 0)
        )
        `when`(farmRepository.findFirstByOwnerIdOrderByCreatedAtAsc(memberId)).thenReturn(farm)
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm)
        `when`(weatherProvider.fetchCurrentWeather(37.5665, 126.9780)).thenReturn(snapshot)

        val result = service.getCurrentWeather(memberId)

        assertThat(result.snapshot).isEqualTo(snapshot)
        assertThat(result.roadAddress).isEqualTo("서울시 강남구")
    }

    @Test
    fun `농지 ID 없이 조회할 때 농지가 없으면 FARM_NOT_FOUND를 던진다`() {
        `when`(farmRepository.findFirstByOwnerIdOrderByCreatedAtAsc(memberId)).thenReturn(null)

        assertThatThrownBy { service.getCurrentWeather(memberId) }
            .isInstanceOf(BusinessException::class.java)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.FARM_NOT_FOUND)

        verifyNoInteractions(weatherProvider)
    }

    @Test
    fun `예보 조회에 성공하면 강수확률과 예보 목록을 채운다`() {
        val farm = Farm(
            id = farmId,
            owner = member,
            name = "약초농장",
            roadAddress = "서울시 강남구",
            latitude = 37.5665,
            longitude = 126.9780
        )
        val snapshot = WeatherSnapshot(
            temperature = 14,
            skyCondition = "맑음",
            observedAt = LocalDateTime.of(2026, 7, 8, 10, 0)
        )
        val dailyForecasts = listOf(
            DailyForecast(
                date = java.time.LocalDate.of(2026, 7, 8),
                minTemperature = 18,
                maxTemperature = 29,
                skyCondition = "맑음"
            )
        )
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm)
        `when`(weatherProvider.fetchCurrentWeather(37.5665, 126.9780)).thenReturn(snapshot)
        `when`(weatherProvider.fetchForecastPanel(37.5665, 126.9780))
            .thenReturn(WeatherForecast(precipitationProbability = 30, dailyForecasts = dailyForecasts))

        val result = service.getCurrentWeather(memberId, farmId)

        assertThat(result.precipitationProbability).isEqualTo(30)
        assertThat(result.forecast).isEqualTo(dailyForecasts)
    }

    @Test
    fun `예보 조회가 실패해도 현재 날씨 응답은 성공하고 예보는 빈 상태로 채워진다`() {
        val farm = Farm(
            id = farmId,
            owner = member,
            name = "약초농장",
            roadAddress = "서울시 강남구",
            latitude = 37.5665,
            longitude = 126.9780
        )
        val snapshot = WeatherSnapshot(
            temperature = 14,
            skyCondition = "맑음",
            observedAt = LocalDateTime.of(2026, 7, 8, 10, 0)
        )
        `when`(farmRepository.findByIdAndOwnerId(farmId, memberId)).thenReturn(farm)
        `when`(weatherProvider.fetchCurrentWeather(37.5665, 126.9780)).thenReturn(snapshot)
        `when`(weatherProvider.fetchForecastPanel(37.5665, 126.9780))
            .thenThrow(BusinessException(ErrorCode.WEATHER_PROVIDER_UNAVAILABLE))

        val result = service.getCurrentWeather(memberId, farmId)

        assertThat(result.snapshot).isEqualTo(snapshot)
        assertThat(result.roadAddress).isEqualTo("서울시 강남구")
        assertThat(result.precipitationProbability).isNull()
        assertThat(result.forecast).isEmpty()
    }
}
