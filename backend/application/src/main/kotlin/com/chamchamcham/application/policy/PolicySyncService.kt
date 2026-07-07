package com.chamchamcham.application.policy

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.application.policy.source.NongupEzPolicySourceClient
import com.chamchamcham.domain.policy.PolicyProgram
import com.chamchamcham.domain.policy.PolicyProgramRepository
import com.chamchamcham.domain.policy.PolicySource
import com.chamchamcham.domain.policy.PolicySyncJob
import com.chamchamcham.domain.policy.PolicySyncJobRepository
import com.chamchamcham.domain.policy.PolicySyncJobStatus
import com.chamchamcham.domain.policy.PolicySyncTriggerType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Year
import java.util.UUID

@Service
@Transactional
class PolicySyncService(
    private val sourceClient: NongupEzPolicySourceClient,
    private val policyProgramRepository: PolicyProgramRepository,
    private val policySyncJobRepository: PolicySyncJobRepository,
    private val cardTextGenerator: PolicyCardTextGenerator,
    private val tagExtractor: NongupEzPolicyTagExtractor,
    private val textListJsonCodec: TextListJsonCodec,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun createAdminSyncJob(adminMemberId: UUID): PolicySyncResult.JobSummary {
        val job = createJobWithDetectedYear(
            triggerType = PolicySyncTriggerType.ADMIN,
            createdByMemberId = adminMemberId
        )
        return PolicySyncResult.JobSummary.from(job)
    }

    fun runScheduledSync(): PolicySyncResult.JobSummary {
        val job = createJobWithDetectedYear(
            triggerType = PolicySyncTriggerType.SCHEDULED,
            createdByMemberId = null
        )
        if (job.status == PolicySyncJobStatus.RUNNING) {
            runExistingJob(requireNotNull(job.id))
        }
        return PolicySyncResult.JobSummary.from(job)
    }

    fun runExistingJob(jobId: UUID) {
        val job = policySyncJobRepository.findById(jobId).orElseThrow {
            BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = jobId)
        }
        try {
            val listItems = sourceClient.fetchPrograms(job.targetYear)
            var synced = 0
            var detailSuccess = 0
            var detailFailure = 0

            listItems.forEach { item ->
                val program = policyProgramRepository.findBySourceAndExternalIdAndSourceYear(
                    PolicySource.NONGUP_EZ,
                    item.externalId,
                    item.sourceYear
                ) ?: PolicyProgram(
                    title = item.title,
                    body = item.summary ?: item.title,
                    region = DEFAULT_REGION,
                    targetManagementType = null
                )

                program.applyListFields(
                    source = PolicySource.NONGUP_EZ,
                    externalId = item.externalId,
                    sourceYear = item.sourceYear,
                    title = item.title,
                    summary = item.summary,
                    region = DEFAULT_REGION,
                    sourceUrl = detailUrl(item.externalId, item.sourceYear),
                    agencyName = item.agencyName,
                    lastSyncedJob = job
                )

                try {
                    val detail = sourceClient.fetchDetail(item.externalId, item.sourceYear)
                    val tags = tagExtractor.extract(detail)
                    val periodLabel = cardTextGenerator.periodLabel(
                        detail.applyStartsOn,
                        detail.applyEndsOn,
                        null
                    )
                    program.applyDetailFields(
                        body = listOfNotNull(detail.purpose, detail.summary, detail.eligibility, detail.benefit)
                            .joinToString("\n\n")
                            .ifBlank { item.summary ?: item.title },
                        purpose = detail.purpose,
                        eligibilityOriginal = detail.eligibility,
                        eligibilitySummary = cardTextGenerator.eligibilitySummary(detail.eligibility),
                        benefitOriginal = detail.benefit,
                        benefitSummary = cardTextGenerator.benefitSummary(detail.benefit),
                        applyStartsOn = detail.applyStartsOn,
                        applyEndsOn = detail.applyEndsOn,
                        applicationPeriodLabel = periodLabel,
                        applicationPeriodNotice = if (periodLabel == UNKNOWN_PERIOD_LABEL) periodLabel else null,
                        applicationMethod = detail.applicationMethod,
                        requiredDocuments = detail.requiredDocuments,
                        selectionCriteria = detail.selectionCriteria,
                        departmentName = detail.contacts.firstOrNull()?.departmentName,
                        onlineApplyAvailable = false,
                        applicationUrl = null,
                        targetTagsJson = textListJsonCodec.encode(tags.targetTags),
                        cropTagsJson = textListJsonCodec.encode(tags.cropTags),
                        regionTagsJson = textListJsonCodec.encode(tags.regionTags),
                        rawPayload = detail.rawJson,
                        recommendable = true,
                        lastSyncedJob = job
                    )
                    detailSuccess += 1
                } catch (exception: Exception) {
                    program.markDetailSyncFailed(rawPayload = item.rawJson)
                    detailFailure += 1
                }

                policyProgramRepository.save(program)
                synced += 1
            }

            job.succeed(
                totalCount = listItems.size,
                syncedCount = synced,
                detailSuccessCount = detailSuccess,
                detailFailureCount = detailFailure
            )
        } catch (exception: Exception) {
            job.fail(exception.message ?: exception.javaClass.simpleName)
        }
    }

    @Transactional(readOnly = true)
    fun getJob(jobId: UUID): PolicySyncResult.JobDetail {
        val job = policySyncJobRepository.findById(jobId).orElseThrow {
            BusinessException(ErrorCode.RESOURCE_NOT_FOUND, detail = jobId)
        }
        return PolicySyncResult.JobDetail.from(job)
    }

    private fun detailUrl(externalId: String, year: String): String =
        "https://www.nongupez.go.kr/nsm/bizAply/wholeBiz/wholeBizDtls?afbzCd=$externalId&bizYr=$year"

    private fun createJobWithDetectedYear(
        triggerType: PolicySyncTriggerType,
        createdByMemberId: UUID?
    ): PolicySyncJob {
        return try {
            policySyncJobRepository.save(
                PolicySyncJob(
                    source = PolicySource.NONGUP_EZ,
                    targetYear = sourceClient.detectLatestYear(),
                    triggerType = triggerType,
                    createdByMemberId = createdByMemberId
                )
            )
        } catch (exception: Exception) {
            val failedJob = PolicySyncJob(
                source = PolicySource.NONGUP_EZ,
                targetYear = Year.now(clock).value.toString(),
                triggerType = triggerType,
                createdByMemberId = createdByMemberId
            )
            failedJob.fail(exception.message ?: exception.javaClass.simpleName)
            policySyncJobRepository.save(failedJob)
        }
    }

    private companion object {
        const val DEFAULT_REGION = "전국"
        const val UNKNOWN_PERIOD_LABEL = "접수기관문의"
    }
}
