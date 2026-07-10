package com.chamchamcham.api.weather

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KmaBaseTimeResolverTest {

    @Test
    fun `초단기실황은 40분 이전이면 직전 정시를 사용한다`() {
        val result = KmaBaseTimeResolver.resolveNcst(LocalDateTime.of(2026, 7, 8, 10, 39))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260708", baseTime = "0900"))
    }

    @Test
    fun `초단기실황은 40분 이후면 현재 정시를 사용한다`() {
        val result = KmaBaseTimeResolver.resolveNcst(LocalDateTime.of(2026, 7, 8, 10, 40))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260708", baseTime = "1000"))
    }

    @Test
    fun `초단기실황은 자정 직후 전날 23시로 롤오버한다`() {
        val result = KmaBaseTimeResolver.resolveNcst(LocalDateTime.of(2026, 7, 8, 0, 10))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260707", baseTime = "2300"))
    }

    @Test
    fun `초단기예보는 45분 이전이면 직전 시각의 30분 발표를 사용한다`() {
        val result = KmaBaseTimeResolver.resolveFcst(LocalDateTime.of(2026, 7, 8, 10, 44))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260708", baseTime = "0930"))
    }

    @Test
    fun `초단기예보는 45분 이후면 현재 시각의 30분 발표를 사용한다`() {
        val result = KmaBaseTimeResolver.resolveFcst(LocalDateTime.of(2026, 7, 8, 10, 45))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260708", baseTime = "1030"))
    }

    @Test
    fun `초단기예보는 자정 30분 이전이면 전날 23시 30분으로 롤오버한다`() {
        val result = KmaBaseTimeResolver.resolveFcst(LocalDateTime.of(2026, 7, 8, 0, 30))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260707", baseTime = "2330"))
    }

    @Test
    fun `동네예보는 10분 지연을 적용해 최신 발표시각을 사용한다`() {
        assertThat(KmaBaseTimeResolver.resolveVilageFcst(LocalDateTime.of(2026, 7, 11, 5, 9)))
            .isEqualTo(KmaBaseDateTime(baseDate = "20260711", baseTime = "0200"))
        assertThat(KmaBaseTimeResolver.resolveVilageFcst(LocalDateTime.of(2026, 7, 11, 5, 10)))
            .isEqualTo(KmaBaseDateTime(baseDate = "20260711", baseTime = "0500"))
    }

    @Test
    fun `동네예보는 첫 발표시각 전이면 전날 23시를 사용한다`() {
        val result = KmaBaseTimeResolver.resolveVilageFcst(LocalDateTime.of(2026, 7, 11, 2, 9))

        assertThat(result).isEqualTo(KmaBaseDateTime(baseDate = "20260710", baseTime = "2300"))
    }
}
