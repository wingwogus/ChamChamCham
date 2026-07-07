package com.chamchamcham.domain.policy

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PolicyRecommendationRepository : JpaRepository<PolicyRecommendation, UUID> {
    @Query(
        """
        select r.policyProgram.id
        from PolicyRecommendation r
        where r.member.id = :memberId
          and r.sourceSyncJob.id = :sourceSyncJobId
        """
    )
    fun findPolicyProgramIdsByMemberIdAndSourceSyncJobId(
        @Param("memberId") memberId: UUID,
        @Param("sourceSyncJobId") sourceSyncJobId: UUID
    ): List<UUID>

    fun deleteByMember_Id(memberId: UUID)
}
