package com.chamchamcham.application.community

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.community.CommunityComment
import com.chamchamcham.domain.community.CommunityCommentRepository
import com.chamchamcham.domain.community.CommunityPostRepository
import com.chamchamcham.domain.member.Member
import com.chamchamcham.domain.member.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class CommunityCommentService(
    private val memberRepository: MemberRepository,
    private val communityPostRepository: CommunityPostRepository,
    private val communityCommentRepository: CommunityCommentRepository
) {
    fun create(command: CommunityCommentCommand.Create): CommunityCommentResult.CommentId {
        val member = memberRepository.findById(command.memberId).orElseThrow {
            BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        }
        val post = communityPostRepository.findByIdAndIsDeletedFalse(command.postId)
            ?: throw BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND)
        val parentComment = command.parentCommentId?.let(::findValidParent)

        val comment = communityCommentRepository.save(
            CommunityComment(
                post = post,
                parentComment = parentComment,
                author = member,
                body = command.body
            )
        )

        return CommunityCommentResult.CommentId(
            requireNotNull(comment.id) { "Persisted comment id is required" }
        )
    }

    fun delete(command: CommunityCommentCommand.Delete) {
        val comment = communityCommentRepository.findById(command.commentId).orElseThrow {
            BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND)
        }
        if (comment.author.id != command.memberId) {
            throw BusinessException(ErrorCode.COMMUNITY_FORBIDDEN)
        }
        comment.softDelete()
    }

    @Transactional(readOnly = true)
    fun list(postId: UUID): List<CommunityCommentResult.Comment> {
        val comments = communityCommentRepository.findByPost_IdOrderByCreatedAtAscIdAsc(postId)
        val repliesByParentId = comments
            .filter { it.parentComment != null }
            .groupBy { requireNotNull(it.parentComment?.id) { "Persisted parent comment id is required" } }

        return comments
            .filter { it.parentComment == null }
            .map { root ->
                toResult(
                    comment = root,
                    replies = repliesByParentId[requireNotNull(root.id) { "Persisted comment id is required" }]
                        .orEmpty()
                        .map { toResult(it) }
                )
            }
    }

    private fun findValidParent(parentCommentId: UUID): CommunityComment {
        val parent = communityCommentRepository.findById(parentCommentId).orElseThrow {
            BusinessException(ErrorCode.COMMUNITY_COMMENT_NOT_FOUND)
        }
        if (parent.isDeleted || parent.parentComment != null) {
            throw BusinessException(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT)
        }
        return parent
    }

    private fun toResult(
        comment: CommunityComment,
        replies: List<CommunityCommentResult.Comment> = emptyList()
    ): CommunityCommentResult.Comment =
        CommunityCommentResult.Comment(
            id = requireNotNull(comment.id) { "Persisted comment id is required" },
            parentCommentId = comment.parentComment?.id,
            author = authorOf(comment.author),
            body = if (comment.isDeleted) DELETED_COMMENT_BODY else comment.body,
            deleted = comment.isDeleted,
            createdAt = comment.createdAt,
            replies = replies
        )

    private fun authorOf(member: Member): CommunityPostResult.AuthorSummary =
        CommunityPostResult.AuthorSummary(
            memberId = requireNotNull(member.id) { "Persisted member id is required" },
            nickname = member.nickname,
            profileImageUrl = member.profileMedia?.fileUrl
        )

    private companion object {
        const val DELETED_COMMENT_BODY = "삭제된 댓글입니다."
    }
}
