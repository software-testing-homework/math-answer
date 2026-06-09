package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookcollection.dto.CommentCreateRequest;
import com.bookcollection.dto.CommentResponse;
import com.bookcollection.entity.SysArticle;
import com.bookcollection.entity.SysComment;
import com.bookcollection.entity.SysCommentLike;
import com.bookcollection.entity.SysUser;
import com.bookcollection.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService 单元测试")
class CommentServiceTest {

    @Mock
    private SysCommentMapper commentMapper;
    @Mock
    private SysCommentLikeMapper commentLikeMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysArticleMapper articleMapper;

    @InjectMocks
    private CommentService commentService;

    // ==================== 辅助方法 ====================

    private SysComment buildComment(Long id, Long articleId, Long userId, String content) {
        SysComment comment = new SysComment();
        comment.setId(id);
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());
        return comment;
    }

    private SysArticle buildArticle(Long id) {
        SysArticle article = new SysArticle();
        article.setId(id);
        article.setCommentCount(3);
        return article;
    }

    private SysUser buildUser(Long id, String nickname, String avatar) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    /** 为 toResponse 中的 userMapper 查询设置桩 */
    private void stubUserForResponse(Long userId) {
        when(userMapper.selectById(userId)).thenReturn(buildUser(userId, "用户" + userId, "avatar.png"));
    }

    // ==================== 1. getCommentList ====================

    @Nested
    @DisplayName("getCommentList — 分页查询评论列表")
    class GetCommentList {

        @Test
        @DisplayName("正常返回分页评论")
        void shouldReturnPagedComments() {
            SysComment comment = buildComment(1L, 10L, 100L, "好文章");
            Page<SysComment> page = new Page<>(1, 10);
            page.setRecords(List.of(comment));
            page.setTotal(1);
            page.setCurrent(1);
            page.setSize(10);
            page.setPages(1);

            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            stubUserForResponse(100L);
            // isLiked 查询（currentUserId != null）
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            Map<String, Object> result = commentService.getCommentList(10L, 1, 10, 100L);

            assertNotNull(result);
            assertEquals(1L, result.get("total"));
            assertEquals(1L, result.get("pageNum"));

            @SuppressWarnings("unchecked")
            List<CommentResponse> list = (List<CommentResponse>) result.get("list");
            assertEquals(1, list.size());
            assertEquals("好文章", list.get(0).getContent());
            assertEquals("用户100", list.get(0).getUserName());
            assertFalse(list.get(0).getIsLiked());
        }

        @Test
        @DisplayName("userId 为 null 时 isLiked 为 false")
        void shouldSetIsLikedFalseWhenUserNull() {
            SysComment comment = buildComment(1L, 10L, 100L, "好文章");
            Page<SysComment> page = new Page<>(1, 10);
            page.setRecords(List.of(comment));
            page.setTotal(1);

            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            stubUserForResponse(100L);

            Map<String, Object> result = commentService.getCommentList(10L, 1, 10, null);

            @SuppressWarnings("unchecked")
            List<CommentResponse> list = (List<CommentResponse>) result.get("list");
            assertFalse(list.get(0).getIsLiked());
            // currentUserId 为 null 时不查 isLiked
            verify(commentLikeMapper, never()).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("已点赞的评论 isLiked 为 true")
        void shouldMarkLikedWhenUserLiked() {
            SysComment comment = buildComment(1L, 10L, 100L, "好文章");
            Page<SysComment> page = new Page<>(1, 10);
            page.setRecords(List.of(comment));
            page.setTotal(1);

            when(commentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            stubUserForResponse(100L);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(new SysCommentLike());

            Map<String, Object> result = commentService.getCommentList(10L, 1, 10, 100L);

            @SuppressWarnings("unchecked")
            List<CommentResponse> list = (List<CommentResponse>) result.get("list");
            assertTrue(list.get(0).getIsLiked());
        }
    }

    // ==================== 2. getTopComments ====================

    @Nested
    @DisplayName("getTopComments — 获取热门评论")
    class GetTopComments {

        @Test
        @DisplayName("正常返回热门评论列表")
        void shouldReturnTopComments() {
            SysComment comment = buildComment(1L, 10L, 100L, "精选评论");
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(comment));
            stubUserForResponse(100L);

            List<CommentResponse> result = commentService.getTopComments(10L, 3, 100L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("精选评论", result.get(0).getContent());
        }

        @Test
        @DisplayName("limit 为 null 时默认 LIMIT 2")
        void shouldDefaultLimit() {
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<CommentResponse> result = commentService.getTopComments(10L, null, null);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 3. createComment ====================

    @Nested
    @DisplayName("createComment — 创建评论")
    class CreateComment {

        @Test
        @DisplayName("正常创建评论并增加文章评论数")
        void shouldCreateCommentAndIncrementCount() {
            SysArticle article = buildArticle(1L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(commentMapper.insert(any(SysComment.class))).thenReturn(1);
            when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            stubUserForResponse(100L);

            CommentCreateRequest request = new CommentCreateRequest();
            request.setArticleId(1L);
            request.setContent("写得好");

            CommentResponse response = commentService.createComment(request, 100L);

            assertNotNull(response);
            assertEquals("写得好", response.getContent());
            assertEquals(1L, response.getArticleId());

            // 验证评论数 +1
            verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
            verify(commentMapper, times(1)).insert(any(SysComment.class));
        }

        @Test
        @DisplayName("文章不存在时抛出异常")
        void shouldThrowWhenArticleNotFound() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            CommentCreateRequest request = new CommentCreateRequest();
            request.setArticleId(999L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.createComment(request, 100L));
            assertEquals("文章不存在", ex.getMessage());
            verify(commentMapper, never()).insert(any());
        }
    }

    // ==================== 4. deleteComment ====================

    @Nested
    @DisplayName("deleteComment — 删除评论")
    class DeleteComment {

        @Test
        @DisplayName("正常删除评论（含点赞级联删除和评论数 -1）")
        void shouldDeleteCommentWithCascade() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(commentMapper.deleteById(1L)).thenReturn(1);
            when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> commentService.deleteComment(1L, 100L));

            verify(commentLikeMapper, times(1)).delete(any(LambdaQueryWrapper.class));
            verify(commentMapper, times(1)).deleteById(1L);
            verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("评论不存在时抛出异常")
        void shouldThrowWhenCommentNotFound() {
            when(commentMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.deleteComment(999L, 100L));
            assertEquals("评论不存在", ex.getMessage());
        }

        @Test
        @DisplayName("非作者无权删除")
        void shouldThrowWhenNotAuthor() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.deleteComment(1L, 999L));
            assertEquals("无权删除此评论", ex.getMessage());
            verify(commentMapper, never()).deleteById(anyLong());
        }
    }

    // ==================== 5. likeComment ====================

    @Nested
    @DisplayName("likeComment — 点赞评论")
    class LikeComment {

        @Test
        @DisplayName("正常点赞")
        void shouldLikeComment() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            when(commentLikeMapper.insert(any(SysCommentLike.class))).thenReturn(1);
            when(commentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> commentService.likeComment(1L, 200L));

            verify(commentLikeMapper, times(1)).insert(any(SysCommentLike.class));
            verify(commentMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("评论不存在时抛出异常")
        void shouldThrowWhenCommentNotFoundForLike() {
            when(commentMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.likeComment(999L, 200L));
            assertEquals("评论不存在", ex.getMessage());
        }

        @Test
        @DisplayName("已点赞时抛出异常")
        void shouldThrowWhenAlreadyLiked() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(new SysCommentLike());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.likeComment(1L, 200L));
            assertEquals("已经点赞过了", ex.getMessage());
        }
    }

    // ==================== 6. unlikeComment ====================

    @Nested
    @DisplayName("unlikeComment — 取消点赞")
    class UnlikeComment {

        @Test
        @DisplayName("正常取消点赞")
        void shouldUnlikeComment() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);

            SysCommentLike existingLike = new SysCommentLike();
            existingLike.setId(5L);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(existingLike);
            when(commentLikeMapper.deleteById(5L)).thenReturn(1);
            when(commentMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> commentService.unlikeComment(1L, 200L));

            verify(commentLikeMapper, times(1)).deleteById(5L);
            verify(commentMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("评论不存在时抛出异常")
        void shouldThrowWhenCommentNotFoundForUnlike() {
            when(commentMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.unlikeComment(999L, 200L));
            assertEquals("评论不存在", ex.getMessage());
        }

        @Test
        @DisplayName("未点赞时抛出异常")
        void shouldThrowWhenNotLiked() {
            SysComment comment = buildComment(1L, 10L, 100L, "test");
            when(commentMapper.selectById(1L)).thenReturn(comment);
            when(commentLikeMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> commentService.unlikeComment(1L, 200L));
            assertEquals("未点赞过此评论", ex.getMessage());
        }
    }
}
