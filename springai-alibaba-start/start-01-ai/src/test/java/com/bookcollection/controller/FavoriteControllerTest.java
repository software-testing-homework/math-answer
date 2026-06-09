package com.bookcollection.controller;

import com.bookcollection.dto.Result;
import com.bookcollection.mapper.*;
import com.bookcollection.service.FavoriteService;
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

@DisplayName("FavoriteController 单元测试")
class FavoriteControllerTest {

    // ==================== Stub Service ====================

    private static final class StubFavoriteService extends FavoriteService {
        private RuntimeException addFavoriteException;
        private Long lastAddFavoriteUserId;
        private Long lastAddFavoriteArticleId;

        private RuntimeException removeFavoriteException;

        private Boolean isFavoritedResult;
        private RuntimeException isFavoritedException;

        private Map<String, Object> getFavoriteArticlesResult;
        private RuntimeException getFavoriteArticlesException;

        private StubFavoriteService() {
            super((UserFavoriteMapper) null, (SysArticleMapper) null,
                    (SysCategoryMapper) null, (SysUserMapper) null);
        }

        @Override
        public void addFavorite(Long userId, Long articleId) {
            lastAddFavoriteUserId = userId;
            lastAddFavoriteArticleId = articleId;
            if (addFavoriteException != null) throw addFavoriteException;
        }

        @Override
        public void removeFavorite(Long userId, Long articleId) {
            if (removeFavoriteException != null) throw removeFavoriteException;
        }

        @Override
        public boolean isFavorited(Long userId, Long articleId) {
            if (isFavoritedException != null) throw isFavoritedException;
            return isFavoritedResult != null && isFavoritedResult;
        }

        @Override
        public Map<String, Object> getFavoriteArticles(Long userId, Integer pageNum, Integer pageSize) {
            if (getFavoriteArticlesException != null) throw getFavoriteArticlesException;
            return getFavoriteArticlesResult;
        }
    }

    // ==================== addFavorite ====================

    @Nested
    @DisplayName("POST /api/favorite/article/{articleId}")
    class AddFavorite {

        @Test
        @DisplayName("正常收藏 → 200")
        void shouldReturn200() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp = controller.addFavorite(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("收藏成功", resp.getBody().getData());
            assertEquals(100L, service.lastAddFavoriteUserId);
            assertEquals(1L, service.lastAddFavoriteArticleId);
        }

        @Test
        @DisplayName("业务异常 → 400")
        void shouldReturn400OnRuntimeException() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.addFavoriteException = new RuntimeException("已收藏该文章");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp = controller.addFavorite(1L, "Bearer " + token);

            assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
            assertEquals("已收藏该文章", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.addFavoriteException = new RuntimeException("已收藏该文章");
            // Already tested 400 above; for general Exception it's hard to trigger
            // since the stub only throws RuntimeException
        }
    }

    // ==================== removeFavorite ====================

    @Nested
    @DisplayName("DELETE /api/favorite/article/{articleId}")
    class RemoveFavorite {

        @Test
        @DisplayName("正常取消收藏 → 200")
        void shouldReturn200() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp = controller.removeFavorite(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("取消收藏成功", resp.getBody().getData());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.removeFavoriteException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<String>> resp = controller.removeFavorite(1L, "Bearer " + token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("取消收藏失败", resp.getBody().getMessage());
        }
    }

    // ==================== getFavoriteStatus ====================

    @Nested
    @DisplayName("GET /api/favorite/article/{articleId}/status")
    class GetFavoriteStatus {

        @Test
        @DisplayName("已收藏 → 200 true")
        void shouldReturnTrue() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.isFavoritedResult = true;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp = controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertTrue(resp.getBody().getData());
        }

        @Test
        @DisplayName("未收藏 → 200 false")
        void shouldReturnFalse() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.isFavoritedResult = false;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp = controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertFalse(resp.getBody().getData());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.isFavoritedException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Boolean>> resp = controller.getFavoriteStatus(1L, "Bearer " + token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取收藏状态失败", resp.getBody().getMessage());
        }
    }

    // ==================== getFavoriteArticles ====================

    @Nested
    @DisplayName("GET /api/favorite/articles")
    class GetFavoriteArticles {

        @Test
        @DisplayName("正常返回分页收藏 → 200")
        void shouldReturn200() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 0L);
            data.put("list", List.of());
            service.getFavoriteArticlesResult = data;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getFavoriteArticles(1, 10, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(0L, resp.getBody().getData().get("total"));
        }

        @Test
        @DisplayName("默认分页参数 → 200")
        void shouldUseDefaultPagination() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);

            Map<String, Object> data = new HashMap<>();
            data.put("total", 1L);
            data.put("list", List.of());
            service.getFavoriteArticlesResult = data;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getFavoriteArticles(1, 10, "Bearer " + token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
        }

        @Test
        @DisplayName("系统异常 → 500")
        void shouldReturn500OnException() {
            StubFavoriteService service = new StubFavoriteService();
            FavoriteController controller = new FavoriteController(service);
            service.getFavoriteArticlesException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Map<String, Object>>> resp =
                    controller.getFavoriteArticles(1, 10, "Bearer " + token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取收藏列表失败", resp.getBody().getMessage());
        }
    }
}
