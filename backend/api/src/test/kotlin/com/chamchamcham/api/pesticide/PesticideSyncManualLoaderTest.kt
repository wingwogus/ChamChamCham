package com.chamchamcham.api.pesticide

import com.chamchamcham.application.pesticide.sync.PesticideSyncService
import com.chamchamcham.domain.pesticide.PesticideSyncJobStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * PSIS 농약등록정보(≈143,912건)를 실 DB에 1회성으로 적재하는 수동 로더.
 *
 * 관리자 토큰/HTTP 없이 실 Postgres에 바로 적재하기 위한 것으로, 일반 테스트 실행(`./gradlew test`)에서는
 * 절대 돌지 않도록 전용 환경변수 `PSIS_PESTICIDE_SYNC_RUN=true`가 있을 때만 활성화된다.
 *
 * 실행 전제(로컬 앱 구동과 동일):
 *  - 로컬 Postgres(5444) + Redis 기동
 *  - env: `PSIS_PESTICIDE_API_KEY`(서비스인증키), `PSIS_PESTICIDE_BASE_URL=http://psis.rda.go.kr/openApi/service.do`
 *
 * 실행 예:
 *  PSIS_PESTICIDE_SYNC_RUN=true \
 *  PSIS_PESTICIDE_API_KEY=<서비스인증키> \
 *  PSIS_PESTICIDE_BASE_URL=http://psis.rda.go.kr/openApi/service.do \
 *  ./gradlew :api:test --tests "com.chamchamcham.api.pesticide.PesticideSyncManualLoaderTest"
 *
 * runExistingJob을 (비동기 러너가 아니라) 직접 호출해 동기로 끝까지 순회하므로, 전량 적재가 끝날 때까지
 * 블로킹된다(수 분~수십 분). 재실행은 dedup되어 안전하다. totalCount에 미달하면 잡이 FAILED가 되어
 * 아래 단언에서 실패로 드러난다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "PSIS_PESTICIDE_SYNC_RUN", matches = "true")
class PesticideSyncManualLoaderTest @Autowired constructor(
    private val pesticideSyncService: PesticideSyncService,
) {
    @Test
    fun `PSIS 농약등록정보를 실 DB에 전량 적재한다`() {
        val job = pesticideSyncService.createSyncJob(adminMemberId = null)
        pesticideSyncService.runExistingJob(job.jobId)

        val detail = pesticideSyncService.getJob(job.jobId)
        println(
            "[PSIS sync] status=${detail.status} total=${detail.totalCount} " +
                "fetched=${detail.fetchedRowCount} createdApplications=${detail.createdApplicationCount} " +
                "error=${detail.errorMessage}"
        )

        assertThat(detail.status).isEqualTo(PesticideSyncJobStatus.SUCCEEDED)
        assertThat(detail.fetchedRowCount).isGreaterThan(0)
    }
}
