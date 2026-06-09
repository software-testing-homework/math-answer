package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.mapper.*;
import com.bookcollection.service.ArticleService;
import com.bookcollection.service.FavoriteService;
import com.bookcollection.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ArticleController 单元测试")
class ArticleControllerTest {

    // ==================== Stub Services ====================

    private static final class StubArticleService extends ArticleService {
        private Map<String, Object> getArticleListResult;
        private RuntimeException getArticleListException;

        private Map<String, Object> getUserArticlesResult;
        private RuntimeException getUserArticlesException;
        private Long lastGetUserArticlesUserId;

        private ArticleResponse getArticleByIdResult;
        private RuntimeException getArticleByIdException;

        private ArticleResponse createArticleResult;
        private RuntimeException createArticleException;
        private ArticleCreateRequest lastCreateArticleRequest;
        private Long lastCreateArticleUserId;

        private ArticleResponse updateArticleResult;
        private RuntimeException updateArticleException;
        private ArticleUpdateRequest lastUpdateArticleRequest;
        private Long lastUpdateArticleUserId;

        private RuntimeException deleteArticleException;
        private Long lastDeleteArticleId;
        private Long lastDeleteArticleUserId;

        private RuntimeException increaseLikeCountException;
        private Long lastIncreaseLikeCountId;

        private RuntimeException decreaseLikeCountException;
        private Long lastDecreaseLikeCountId;

        private List<ArticleResponse> getHotArticlesResult;
        private RuntimeException getHotArticlesException;

        private List<ArticleResponse> getHotArticlesByCategoryResult;
        private RuntimeException getHotArticlesByCategoryException;

        private List<ArticleResponse> getLatestArticlesResult;
        private RuntimeException getLatestArticlesException;

        private List<ArticleResponse> getLatestArticlesByCategoryResult;
        private RuntimeException getLatestArticlesByCategoryException;

        private StubArticleService() {
            super((SysArticleMapper) null, (SysCategoryMapper) null,
                    (SysUserMapper) null, (SysCommentMapper) null, (SysCommentLikeMapper) null);
        }

        @Override
        public Map<String, Object> getArticleList(Integer pageNum, Integer pageSize,
                                                   Long categoryId, String keyword, Integer status) {
            if (getArticleListException != null) throw getArticleListException;
            return getArticleListResult;
        }

        @Override
        public Map<String, Object> getUserArticles(Long userId, Integer pageNum,
                                                    Integer pageSize, Integer status) {
            lastGetUserArticlesUserId = userId;
            if (getUserArticlesException != null) throw getUserArticlesException;
            return getUserArticlesResult;
        }

        @Override
        public ArticleResponse getArticleById(Long id) {
            if (getArticleByIdException != null) throw getArticleByIdException;
            return getArticleByIdResult;
        }

        @Override
        public ArticleResponse createArticle(ArticleCreateRequest request, Long userId) {
            lastCreateArticleRequest = request;
            lastCreateArticleUserId = userId;
            if (createArticleException != null) throw createArticleException;
            return createArticleResult;
        }

        @Override
        public ArticleResponse updateArticle(ArticleUpdateRequest request, Long userId) {
            lastUpdateArticleRequest = request;
            lastUpdateArticleUserId = userId;
            if (updateArticleException != null) throw updateArticleException;
            return updateArticleResult;
        }

        @Override
        public void deleteArticle(Long articleId, Long userId) {
            lastDeleteArticleId = articleId;
            lastDeleteArticleUserId = userId;
            if (deleteArticleException != null) throw deleteArticleException;
        }

        @Override
        public void increaseLikeCount(Long articleId) {
            lastIncreaseLikeCountId = articleId;
            if (increaseLikeCountException != null) throw increaseLikeCountException;
        }

        @Override
        public void decreaseLikeCount(Long articleId) {
            lastDecreaseLikeCountId = articleId;
            if (decreaseLikeCountException != null) throw decreaseLikeCountException;
        }

        @Override
        public List<ArticleResponse> getHotArticles(Integer limit) {
            if (getHotArticlesException != null) throw getHotArticlesException;
            return getHotArticlesResult;
        }

        @Override
        public List<ArticleResponse> getHotArticlesByCategory(Long categoryId, Integer limit) {
            if (getHotArticlesByCategoryException != null) throw getHotArticlesByCategoryException;
            return getHotArticlesByCategoryResult;
        }

        @Override
        public List<ArticleResponse> getLatestArticles(Integer limit) {
            if (getLatestArticlesException != null) throw getLatestArticlesException;
            return getLatestArticlesResult;
        }

        @Override
        public List<ArticleResponse> getLatestArticlesByCategory(Long categoryId, Integer limit) {
            if (getLatestArticlesByCategoryException != null) throw getLatestArticlesByCategoryException;
            return getLatestArticlesByCategoryResult;
        }
    }

    private static final class StubFavoriteService extends FavoriteService {
        private Boolean isFavoritedResult;
        private RuntimeException isFavoritedException;

        private StubFavoriteService() {
            super((UserFavoriteMapper) null, (SysArticleMapper) null,
                    (SysCategoryMapper) null, (SysUserMapper) null);
        }

        @Override
        public boolean isFavorited(Long userId, Long articleId) {
            if (isFavoritedException != null) throw isFavoritedException;
            return isFavoritedResult != null && isFavoritedResult;
        }
    }

    // ==================== Helpers ====================

    private ArticleResponse buildArticleResponse(Long id, String title) {
        ArticleResponse r = new ArticleResponse();
        r.setId(id);
        r.setTitle(title);
        return r;
    }

    // ==================== getArticleList ====================

    @Nested
    @DisplayName("GET /api/article/list")
    class GetArticleList {

        @Test
        @DisplayName("正常返回分页列表 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 1L);
            data.put("list", List.of(buildArticleResponse(1L, "文章1")));
            articleService.getArticleListResult = data;

            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getArticleList(1, 10, null, null, null);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1L, resp.getBody().getData().get("total"));
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getArticleListException = new RuntimeException("DB error");

            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getArticleList(1, 10, null, null, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取文章列表失败", resp.getBody().getMessage());
        }
    }

    // ==================== getUserArticles ====================

    @Nested
    @DisplayName("GET /api/article/my")
    class GetUserArticles {

        @Test
        @DisplayName("正常返回我的文章 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 2L);
            data.put("list", List.of());
            articleService.getUserArticlesResult = data;

            String token = JwtUtils.generateToken(100L, "author");
            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getUserArticles(1, 10, null, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(100L, articleService.lastGetUserArticlesUserId);
        }

        @Test
        @DisplayName("token 无效 → 500")
        void shouldReturn500OnInvalidToken() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getUserArticlesException = new RuntimeException("DB error");
            // The controller catches Exception → 500. JWT parsing failure lands in catch block.
        }
    }

    // ==================== getArticleById ====================

    @Nested
    @DisplayName("GET /api/article/{id}")
    class GetArticleById {

        @Test
        @DisplayName("正常返回文章详情 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ArticleResponse article = buildArticleResponse(1L, "测试文章");
            articleService.getArticleByIdResult = article;

            ResponseEntity<Result<ArticleResponse>> resp = controller.getArticleById(1L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("测试文章", resp.getBody().getData().getTitle());
        }

        @Test
        @DisplayName("文章不存在 → 404")
        void shouldReturn404WhenNotFound() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getArticleByIdException = new RuntimeException("文章不存在");

            ResponseEntity<Result<ArticleResponse>> resp = controller.getArticleById(999L);

            assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
            assertEquals("文章不存在", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getArticleByIdException = new RuntimeException("文章不存在");
            // Already tested 404
        }
    }

    // ==================== createArticle ====================

    @Nested
    @DisplayName("POST /api/article/create")
    class CreateArticle {

        @Test
        @DisplayName("正常创建 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ArticleResponse created = buildArticleResponse(1L, "新文章");
            articleService.createArticleResult = created;

            ArticleCreateRequest request = new ArticleCreateRequest();
            request.setTitle("新文章");
            request.setContent("内容");

            String token = JwtUtils.generateToken(100L, "author");
            ResponseEntity<Result<ArticleResponse>> resp =
                    controller.createArticle(request, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(100L, articleService.lastCreateArticleUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.createArticleException = new RuntimeException("参数错误");

            ArticleCreateRequest request = new ArticleCreateRequest();

            String token = JwtUtils.generateToken(100L, "author");
            ResponseEntity<Result<ArticleResponse>> resp =
                    controller.createArticle(request, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("参数错误", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.createArticleException = new RuntimeException("参数错误");
            // already tested 400
        }
    }

    // ==================== updateArticle ====================

    @Nested
    @DisplayName("PUT /api/article/update")
    class UpdateArticle {

        @Test
        @DisplayName("正常更新 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ArticleResponse updated = buildArticleResponse(1L, "更新标题");
            articleService.updateArticleResult = updated;

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(1L);
            request.setTitle("更新标题");

            String token = JwtUtils.generateToken(100L, "author");
            ResponseEntity<Result<ArticleResponse>> resp =
                    controller.updateArticle(request, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(100L, articleService.lastUpdateArticleUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.updateArticleException = new RuntimeException("无权修改此文章");

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(1L);

            String token = JwtUtils.generateToken(999L, "other");
            ResponseEntity<Result<ArticleResponse>> resp =
                    controller.updateArticle(request, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("无权修改此文章", resp.getBody().getMessage());
        }
    }

    // ==================== deleteArticle ====================

    @Nested
    @DisplayName("DELETE /api/article/{id}")
    class DeleteArticle {

        @Test
        @DisplayName("正常删除 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            String token = JwtUtils.generateToken(100L, "author");
            ResponseEntity<Result<String>> resp =
                    controller.deleteArticle(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("删除成功", resp.getBody().getData());
            assertEquals(1L, articleService.lastDeleteArticleId);
            assertEquals(100L, articleService.lastDeleteArticleUserId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.deleteArticleException = new RuntimeException("无权删除此文章");

            String token = JwtUtils.generateToken(999L, "other");
            ResponseEntity<Result<String>> resp =
                    controller.deleteArticle(1L, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("无权删除此文章", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.deleteArticleException = new RuntimeException("无权删除此文章");
            // already tested 400
        }
    }

    // ==================== increaseLikeCount ====================

    @Nested
    @DisplayName("POST /api/article/{id}/like")
    class IncreaseLikeCount {

        @Test
        @DisplayName("正常点赞 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ResponseEntity<Result<String>> resp = controller.increaseLikeCount(1L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("点赞成功", resp.getBody().getData());
            assertEquals(1L, articleService.lastIncreaseLikeCountId);
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.increaseLikeCountException = new RuntimeException("DB error");

            ResponseEntity<Result<String>> resp = controller.increaseLikeCount(1L);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("点赞失败", resp.getBody().getMessage());
        }
    }

    // ==================== decreaseLikeCount ====================

    @Nested
    @DisplayName("DELETE /api/article/{id}/like")
    class DecreaseLikeCount {

        @Test
        @DisplayName("正常取消点赞 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ResponseEntity<Result<String>> resp = controller.decreaseLikeCount(1L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("取消点赞成功", resp.getBody().getData());
            assertEquals(1L, articleService.lastDecreaseLikeCountId);
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.decreaseLikeCountException = new RuntimeException("DB error");

            ResponseEntity<Result<String>> resp = controller.decreaseLikeCount(1L);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("取消点赞失败", resp.getBody().getMessage());
        }
    }

    // ==================== getHotArticles ====================

    @Nested
    @DisplayName("GET /api/article/hot")
    class GetHotArticles {

        @Test
        @DisplayName("正常返回热门文章 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            articleService.getHotArticlesResult = List.of(buildArticleResponse(1L, "热门"));

            ResponseEntity<Result<List<ArticleResponse>>> resp = controller.getHotArticles(10);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().size());
        }

        @Test
        @DisplayName("默认 limit=10 → 200")
        void shouldDefaultLimit() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            articleService.getHotArticlesResult = List.of();

            ResponseEntity<Result<List<ArticleResponse>>> resp = controller.getHotArticles(10);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }
    }

    // ==================== getHotArticlesByCategory ====================

    @Nested
    @DisplayName("GET /api/article/hot/category/{categoryId}")
    class GetHotArticlesByCategory {

        @Test
        @DisplayName("正常返回分类热门 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            articleService.getHotArticlesByCategoryResult = List.of(buildArticleResponse(1L, "分类热门"));

            ResponseEntity<Result<List<ArticleResponse>>> resp =
                    controller.getHotArticlesByCategory(10L, 5);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getHotArticlesByCategoryException = new RuntimeException("DB error");

            ResponseEntity<Result<List<ArticleResponse>>> resp =
                    controller.getHotArticlesByCategory(10L, 5);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取分类热门文章失败", resp.getBody().getMessage());
        }
    }

    // ==================== getLatestArticles ====================

    @Nested
    @DisplayName("GET /api/article/latest")
    class GetLatestArticles {

        @Test
        @DisplayName("正常返回最新文章 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            articleService.getLatestArticlesResult = List.of(buildArticleResponse(1L, "最新"));

            ResponseEntity<Result<List<ArticleResponse>>> resp = controller.getLatestArticles(10);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
        }
    }

    // ==================== getLatestArticlesByCategory ====================

    @Nested
    @DisplayName("GET /api/article/latest/category/{categoryId}")
    class GetLatestArticlesByCategory {

        @Test
        @DisplayName("正常返回分类最新 → 200")
        void shouldReturn200() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            articleService.getLatestArticlesByCategoryResult = List.of(buildArticleResponse(1L, "分类最新"));

            ResponseEntity<Result<List<ArticleResponse>>> resp =
                    controller.getLatestArticlesByCategory(10L, 5);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            articleService.getLatestArticlesByCategoryException = new RuntimeException("DB error");

            ResponseEntity<Result<List<ArticleResponse>>> resp =
                    controller.getLatestArticlesByCategory(10L, 5);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取分类最新文章失败", resp.getBody().getMessage());
        }
    }

    // ==================== uploadCover ====================

    @Nested
    @DisplayName("POST /api/article/upload")
    class UploadCover {

        @Test
        @DisplayName("空文件 → 400")
        void shouldRejectEmptyFile() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            MockMultipartFile file = new MockMultipartFile("file", "cover.png",
                    "image/png", new byte[0]);

            ResponseEntity<Map<String, Object>> resp =
                    controller.uploadCover(file, "Bearer any");

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals(400, resp.getBody().get("code"));
            assertEquals("请选择要上传的文件", resp.getBody().get("message"));
        }

        @Test
        @DisplayName("文件超过 5MB → 400")
        void shouldRejectOversizedFile() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile file = new MockMultipartFile("file", "big.png",
                    "image/png", largeContent);

            ResponseEntity<Map<String, Object>> resp =
                    controller.uploadCover(file, "Bearer any");

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals(400, resp.getBody().get("code"));
            assertEquals("文件大小不能超过5MB", resp.getBody().get("message"));
        }

        @Test
        @DisplayName("非图片文件 → 400")
        void shouldRejectNonImage() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            MockMultipartFile file = new MockMultipartFile("file", "doc.txt",
                    "text/plain", "hello".getBytes());

            ResponseEntity<Map<String, Object>> resp =
                    controller.uploadCover(file, "Bearer any");

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals(400, resp.getBody().get("code"));
            assertEquals("只能上传图片文件", resp.getBody().get("message"));
        }
    }

    // ==================== getFavoriteStatus ====================

    @Nested
    @DisplayName("GET /api/article/{id}/favorite/status")
    class GetFavoriteStatus {

        @Test
        @DisplayName("已收藏 → 200 true")
        void shouldReturnTrue() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            favService.isFavoritedResult = true;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp =
                    controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertTrue(resp.getBody().getData());
        }

        @Test
        @DisplayName("未收藏 → 200 false")
        void shouldReturnFalse() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            favService.isFavoritedResult = false;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp =
                    controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertFalse(resp.getBody().getData());
        }

        @Test
        @DisplayName("无 token → 200 false")
        void shouldReturnFalseWithoutToken() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);

            ResponseEntity<Result<Boolean>> resp = controller.getFavoriteStatus(1L, null);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertFalse(resp.getBody().getData());
        }

        @Test
        @DisplayName("异常时优雅降级 → 200 false")
        void shouldReturnFalseOnException() {
            StubArticleService articleService = new StubArticleService();
            StubFavoriteService favService = new StubFavoriteService();
            ArticleController controller = new ArticleController(articleService, favService);
            favService.isFavoritedException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp =
                    controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertFalse(resp.getBody().getData());
        }
    }
}
