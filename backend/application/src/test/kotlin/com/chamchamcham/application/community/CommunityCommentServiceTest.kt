package com.chamchamcham.application.community

import com.chamchamcham.application.exception.ErrorCode
import com.chamchamcham.application.exception.business.BusinessException
import com.chamchamcham.domain.common.BaseTimeEntity
import com.chamchamcham.domain.community.CommunityComment
import com.chamchamcham.domain.community.CommunityCommentRepository
import com.chamchamcham.domain.community.CommunityPost
import com.chamchamcham.domain.community.CommunityPostRepository
import com.chamchamcham.domain.community.CommunityPostType
import com.chamchamcham.domain.crop.Crop
import com.chamchamcham.domain.crop.CropUsePartCategory
import com.chamchamcham.domain.member.Member
import com.chamchamcham.domain.member.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CommunityCommentServiceTest {
    private val memberId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val otherMemberId = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val postId = UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val rootCommentId = UUID.fromString("00000000-0000-0000-0000-000000000201")
    private val replyCommentId = UUID.fromString("00000000-0000-0000-0000-000000000202")

    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var communityPostRepository: CommunityPostRepository
    @Mock private lateinit var communityCommentRepository: CommunityCommentRepository

    private lateinit var service: CommunityCommentService
    private lateinit var member: Member
    private lateinit var otherMember: Member
    private lateinit var post: CommunityPost
    private lateinit var rootComment: CommunityComment
    private lateinit var replyComment: CommunityComment

    @BeforeEach
    fun setUp() {
        member = Member(id = memberId, email = "farmer@example.com", passwordHash = null)
        otherMember = Member(id = otherMemberId, email = "other@example.com", passwordHash = null)
        post = CommunityPost(
            id = postId,
            author = member,
            crop = Crop(
                id = UUID.fromString("00000000-0000-0000-0000-000000000301"),
                externalNo = 422,
                name = "황기",
                usePartCategory = CropUsePartCategory.ROOT_BARK
            ),
            postType = CommunityPostType.QUESTION,
            title = "황기 발아율이 낮아요",
            body = "싹이 거의 올라오지 않아요."
        )
        rootComment = comment(id = rootCommentId, parent = null, author = member, body = "저도 궁금해요")
        replyComment = comment(id = replyCommentId, parent = rootComment, author = otherMember, body = "스크래치 작업을 해보세요")

        service = CommunityCommentService(
            memberRepository = memberRepository,
            communityPostRepository = communityPostRepository,
            communityCommentRepository = communityCommentRepository
        )
    }

    @Test
    fun `create root comment stores comment on active post`() {
        stubCreate()
        `when`(communityCommentRepository.save(any(CommunityComment::class.java))).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as CommunityComment
            comment(id = rootCommentId, parent = comment.parentComment, author = comment.author, body = comment.body)
        }

        val result = service.create(createCommand(parentCommentId = null, body = "저도 궁금해요"))

        assertEquals(rootCommentId, result.id)
        val savedComment = capturedComment()
        assertEquals(postId, savedComment.post.id)
        assertNull(savedComment.parentComment)
        assertEquals(memberId, savedComment.author.id)
        assertEquals("저도 궁금해요", savedComment.body)
    }

    @Test
    fun `create reply stores one level reply under root comment`() {
        stubCreate()
        `when`(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(rootComment))
        `when`(communityCommentRepository.save(any(CommunityComment::class.java))).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as CommunityComment
            comment(id = replyCommentId, parent = comment.parentComment, author = comment.author, body = comment.body)
        }

        val result = service.create(createCommand(parentCommentId = rootCommentId, body = "스크래치 작업을 해보세요"))

        assertEquals(replyCommentId, result.id)
        val savedComment = capturedComment()
        assertEquals(rootCommentId, savedComment.parentComment?.id)
        assertEquals("스크래치 작업을 해보세요", savedComment.body)
    }

    @Test
    fun `create reply rejects reply parent that is already a reply`() {
        stubCreate()
        `when`(communityCommentRepository.findById(replyCommentId)).thenReturn(Optional.of(replyComment))

        val exception = assertThrows(BusinessException::class.java) {
            service.create(createCommand(parentCommentId = replyCommentId))
        }

        assertEquals(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT, exception.errorCode)
        verify(communityCommentRepository, never()).save(any(CommunityComment::class.java))
    }

    @Test
    fun `create reply rejects deleted parent comment`() {
        val deletedParent = comment(id = rootCommentId, parent = null, author = member, body = "삭제 전", isDeleted = true)
        stubCreate()
        `when`(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(deletedParent))

        val exception = assertThrows(BusinessException::class.java) {
            service.create(createCommand(parentCommentId = rootCommentId))
        }

        assertEquals(ErrorCode.COMMUNITY_INVALID_REPLY_PARENT, exception.errorCode)
        verify(communityCommentRepository, never()).save(any(CommunityComment::class.java))
    }

    @Test
    fun `delete soft deletes author comment`() {
        `when`(communityCommentRepository.findById(rootCommentId)).thenReturn(Optional.of(rootComment))

        service.delete(CommunityCommentCommand.Delete(memberId = memberId, commentId = rootCommentId))

        assertTrue(rootComment.isDeleted)
    }

    @Test
    fun `list returns deleted comment body as deleted message`() {
        rootComment.softDelete()
        setCreatedAt(rootComment, LocalDateTime.of(2026, 6, 12, 9, 0))
        setCreatedAt(replyComment, LocalDateTime.of(2026, 6, 12, 9, 1))
        `when`(communityCommentRepository.findByPost_IdOrderByCreatedAtAscIdAsc(postId))
            .thenReturn(listOf(rootComment, replyComment))

        val comments = service.list(postId)

        assertEquals("삭제된 댓글입니다.", comments.first { it.id == rootCommentId }.body)
        assertTrue(comments.first { it.id == rootCommentId }.deleted)
    }

    private fun stubCreate() {
        `when`(memberRepository.findById(memberId)).thenReturn(Optional.of(member))
        `when`(communityPostRepository.findByIdAndIsDeletedFalse(postId)).thenReturn(post)
    }

    private fun createCommand(
        parentCommentId: UUID? = null,
        body: String = "저도 궁금해요"
    ): CommunityCommentCommand.Create =
        CommunityCommentCommand.Create(
            memberId = memberId,
            postId = postId,
            parentCommentId = parentCommentId,
            body = body
        )

    private fun comment(
        id: UUID,
        parent: CommunityComment?,
        author: Member,
        body: String,
        isDeleted: Boolean = false
    ): CommunityComment =
        CommunityComment(
            id = id,
            post = post,
            parentComment = parent,
            author = author,
            body = body,
            isDeleted = isDeleted
        )

    private fun capturedComment(): CommunityComment {
        val captor = ArgumentCaptor.forClass(CommunityComment::class.java)
        verify(communityCommentRepository).save(captor.capture())
        return captor.value
    }

    private fun setCreatedAt(entity: BaseTimeEntity, createdAt: LocalDateTime) {
        val field = BaseTimeEntity::class.java.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(entity, createdAt)
    }
}
