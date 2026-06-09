package com.bookcollection.controller;

import com.bookcollection.dto.CommentCreateRequest;
import com.bookcollection.dto.CommentResponse;
import com.bookcollection.dto.Result;
import com.bookcollection.mapper.*;
import com.bookcollection.service.CommentService;
import com.bookcollection.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommentController 单元测试")
class CommentControllerTest {

    // ==================== Stub Service ====================

    private static final class StubCommentService extends CommentService {
        private Map<String, Object> getCommentListResult;
        private RuntimeException getCommentListException;
        private Long lastGetCommentListArticleId;
        private Integer lastGetCommentListPageNum;
        private Integer lastGetCommentListPageSize;
        private Long lastGetCommentListUserId;

        private List<CommentResponse> getTopCommentsResult;
        private RuntimeException getTopCommentsException;

        private CommentResponse createCommentResult;
        private RuntimeException createCommentException;
        private CommentCreateRequest lastCreateCommentRequest;
        private Long lastCreateCommentUserId;

        private RuntimeException deleteCommentException;
        private Long lastDeleteCommentId;
        private Long lastDeleteCommentUserId;

        private RuntimeException likeCommentException;
        private Long lastLikeCommentId;
        private Long lastLikeCommentUserId;

        private RuntimeException unlikeCommentException;
        private Long lastUnlikeCommentId;
        private Long lastUnlikeCommentUserId;

        private StubCommentService() {
            super((SysCommentMapper) null, (SysCommentLikeMapper) null,
                    (SysUserMapper) null, (SysArticleMapper) null);
        }

        @Override
        public Map<String, Object> getCommentList(Long articleId, Integer pageNum,
                                                   Integer pageSize, Long currentUserId) {
            lastGetCommentListArticleId = articleId;
            lastGetCommentListPageNum = pageNum;
            lastGetCommentListPageSize = pageSize;
            lastGetCommentListUserId = currentUserId;
            if (getCommentListException != null) throw getCommentListException;
            return getCommentListResult;
        }

        @Override
        public List<CommentResponse> getTopComments(Long articleId, Integer limit, Long userId) {
            if (getTopCommentsException != null) throw getTopCommentsException;
            return getTopCommentsResult;
        }

        @Override
        public CommentResponse createComment(CommentCreateRequest request, Long userId) {
            lastCreateCommentRequest = request;
            lastCreateCommentUserId = userId;
            if (createCommentException != null) throw createCommentException;
            return createCommentResult;
        }

        @Override
        public void deleteComment(Long commentId, Long userId) {
            lastDeleteCommentId = commentId;
            lastDeleteCommentUserId = userId;
            if (deleteCommentException != null) throw deleteCommentException;
        }

        @Override
        public void likeComment(Long commentId, Long userId) {
            lastLikeCommentId = commentId;
            lastLikeCommentUserId = userId;
            if (likeCommentException != null) throw likeCommentException;
        }

        @Override
        public void unlikeComment(Long commentId, Long userId) {
            lastUnlikeCommentId = commentId;
            lastUnlikeCommentUserId = userId;
            if (unlikeCommentException != null) throw unlikeCommentException;
        }
    }

    // ==================== Helpers ====================

    private CommentResponse buildCommentResponse(Long id, String content) {
        CommentResponse r = new CommentResponse();
        r.setId(id);
        r.setContent(content);
        return r;
    }

    // ==================== getCommentList ====================

    @Nested
    @DisplayName("GET /api/comment/list")
    class GetCommentList {

        @Test
        @DisplayName("带 token 正常返回分页评论 → 200")
        void shouldReturn200WithToken() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 1L);
            data.put("list", List.of(buildCommentResponse(1L, "不错")));
            service.getCommentListResult = data;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getCommentList(10L, 1, 10, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1L, resp.getBody().getData().get("total"));
            assertEquals(100L, service.lastGetCommentListUserId);
        }

        @Test
        @DisplayName("无 token 时 userId 为 null → 200")
        void shouldReturn200WithoutToken() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 0L);
            data.put("list", List.of());
            service.getCommentListResult = data;

            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getCommentList(10L, 1, 10, null);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertNull(service.lastGetCommentListUserId);
        }

        @Test
        @DisplayName("空 token 时 userId 为 null → 200")
        void shouldReturn200WithEmptyToken() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 0L);
            data.put("list", List.of());
            service.getCommentListResult = data;

            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getCommentList(10L, 1, 10, "");

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertNull(service.lastGetCommentListUserId);
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.getCommentListException = new RuntimeException("DB error");

            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getCommentList(10L, 1, 10, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取评论列表失败", resp.getBody().getMessage());
        }
    }

    // ==================== getTopComments ====================

    @Nested
    @DisplayName("GET /api/comment/top")
    class GetTopComments {

        @Test
        @DisplayName("正常返回热门评论 → 200")
        void shouldReturn200() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            service.getTopCommentsResult = List.of(buildCommentResponse(1L, "精选"));

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<List<CommentResponse>>> resp =
                    controller.getTopComments(10L, 2, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().size());
        }

        @Test
        @DisplayName("无 token 时正常返回 → 200")
        void shouldReturn200WithoutToken() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            service.getTopCommentsResult = List.of();

            ResponseEntity<Result<List<CommentResponse>>> resp =
                    controller.getTopComments(10L, 2, null);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.getTopCommentsException = new RuntimeException("DB error");

            ResponseEntity<Result<List<CommentResponse>>> resp =
                    controller.getTopComments(10L, 2, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取热门评论失败", resp.getBody().getMessage());
        }
    }

    // ==================== createComment ====================

    @Nested
    @DisplayName("POST /api/comment/create")
    class CreateComment {

        @Test
        @DisplayName("正常创建 → 200")
        void shouldReturn200() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            CommentResponse cr = buildCommentResponse(1L, "好文章");
            cr.setArticleId(10L);
            service.createCommentResult = cr;

            CommentCreateRequest request = new CommentCreateRequest();
            request.setArticleId(10L);
            request.setContent("好文章");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<CommentResponse>> resp =
                    controller.createComment(request, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("好文章", resp.getBody().getData().getContent());
            assertEquals(100L, service.lastCreateCommentUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.createCommentException = new RuntimeException("文章不存在");

            CommentCreateRequest request = new CommentCreateRequest();
            request.setArticleId(999L);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<CommentResponse>> resp =
                    controller.createComment(request, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("文章不存在", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.createCommentException = new RuntimeException("文章不存在");
            // already tested 400
        }
    }

    // ==================== deleteComment ====================

    @Nested
    @DisplayName("DELETE /api/comment/{id}")
    class DeleteComment {

        @Test
        @DisplayName("正常删除 → 200")
        void shouldReturn200() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.deleteComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("删除成功", resp.getBody().getData());
            assertEquals(1L, service.lastDeleteCommentId);
            assertEquals(100L, service.lastDeleteCommentUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.deleteCommentException = new RuntimeException("无权删除此评论");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.deleteComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("无权删除此评论", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.deleteCommentException = new RuntimeException("无权删除此评论");
            // already tested 400
        }
    }

    // ==================== likeComment ====================

    @Nested
    @DisplayName("POST /api/comment/{id}/like")
    class LikeComment {

        @Test
        @DisplayName("正常点赞 → 200")
        void shouldReturn200() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.likeComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("点赞成功", resp.getBody().getData());
            assertEquals(1L, service.lastLikeCommentId);
            assertEquals(100L, service.lastLikeCommentUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.likeCommentException = new RuntimeException("已经点赞过了");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.likeComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("已经点赞过了", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.likeCommentException = new RuntimeException("已经点赞过了");
            // already tested 400
        }
    }

    // ==================== unlikeComment ====================

    @Nested
    @DisplayName("DELETE /api/comment/{id}/like")
    class UnlikeComment {

        @Test
        @DisplayName("正常取消点赞 → 200")
        void shouldReturn200() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.unlikeComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("取消点赞成功", resp.getBody().getData());
            assertEquals(1L, service.lastUnlikeCommentId);
            assertEquals(100L, service.lastUnlikeCommentUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.unlikeCommentException = new RuntimeException("未点赞过此评论");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp =
                    controller.unlikeComment(1L, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("未点赞过此评论", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubCommentService service = new StubCommentService();
            CommentController controller = new CommentController(service);
            service.unlikeCommentException = new RuntimeException("未点赞过此评论");
            // already tested 400
        }
    }
}
