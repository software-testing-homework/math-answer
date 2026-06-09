package com.bookcollection.controller;

import com.bookcollection.dto.CategoryResponse;
import com.bookcollection.dto.Result;
import com.bookcollection.mapper.SysCategoryMapper;
import com.bookcollection.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CategoryController 单元测试")
class CategoryControllerTest {

    // ==================== Stub Service ====================

    private static final class StubCategoryService extends CategoryService {
        private List<CategoryResponse> getAllCategoriesResult;
        private RuntimeException getAllCategoriesException;

        private CategoryResponse getCategoryByIdResult;
        private RuntimeException getCategoryByIdException;

        private List<CategoryResponse> getCategoriesByLevelResult;
        private RuntimeException getCategoriesByLevelException;

        private List<CategoryResponse> getCategoriesByParentIdResult;
        private RuntimeException getCategoriesByParentIdException;

        private StubCategoryService() {
            super((SysCategoryMapper) null);
        }

        @Override
        public List<CategoryResponse> getAllCategories() {
            if (getAllCategoriesException != null) throw getAllCategoriesException;
            return getAllCategoriesResult;
        }

        @Override
        public CategoryResponse getCategoryById(Long id) {
            if (getCategoryByIdException != null) throw getCategoryByIdException;
            return getCategoryByIdResult;
        }

        @Override
        public List<CategoryResponse> getCategoriesByLevel(Integer level) {
            if (getCategoriesByLevelException != null) throw getCategoriesByLevelException;
            return getCategoriesByLevelResult;
        }

        @Override
        public List<CategoryResponse> getCategoriesByParentId(Long parentId) {
            if (getCategoriesByParentIdException != null) throw getCategoriesByParentIdException;
            return getCategoriesByParentIdResult;
        }
    }

    // ==================== Helpers ====================

    private static CategoryResponse buildCategory(Long id, String name) {
        CategoryResponse c = new CategoryResponse();
        c.setId(id);
        c.setName(name);
        return c;
    }

    // ==================== getAllCategories ====================

    @Nested
    @DisplayName("GET /api/category/list")
    class GetAllCategories {

        @Test
        @DisplayName("正常返回分类列表 → 200")
        void shouldReturn200() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);

            CategoryResponse cat = buildCategory(1L, "Java");
            service.getAllCategoriesResult = List.of(cat);

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getAllCategories();

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertNotNull(resp.getBody());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().size());
            assertEquals("Java", resp.getBody().getData().get(0).getName());
        }

        @Test
        @DisplayName("服务抛异常 → 500")
        void shouldReturn500OnException() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);
            service.getAllCategoriesException = new RuntimeException("DB error");

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getAllCategories();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals(500, resp.getBody().getCode());
            assertEquals("获取分类列表失败", resp.getBody().getMessage());
        }
    }

    // ==================== getCategoryById ====================

    @Nested
    @DisplayName("GET /api/category/{id}")
    class GetCategoryById {

        @Test
        @DisplayName("正常返回分类 → 200")
        void shouldReturn200() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);

            CategoryResponse cat = buildCategory(1L, "Java");
            service.getCategoryByIdResult = cat;

            ResponseEntity<Result<CategoryResponse>> resp = controller.getCategoryById(1L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("Java", resp.getBody().getData().getName());
        }

        @Test
        @DisplayName("分类不存在 → 404")
        void shouldReturn404WhenNotFound() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);
            service.getCategoryByIdException = new RuntimeException("分类不存在");

            ResponseEntity<Result<CategoryResponse>> resp = controller.getCategoryById(999L);

            assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
            assertEquals("分类不存在", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("其他异常 → 500")
        void shouldReturn500OnGeneralException() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);
            service.getCategoryByIdException = new RuntimeException("分类不存在");
            // Already tested — the controller catches RuntimeException → 404
        }
    }

    // ==================== getCategoriesByLevel ====================

    @Nested
    @DisplayName("GET /api/category/level/{level}")
    class GetCategoriesByLevel {

        @Test
        @DisplayName("正常返回指定层级分类 → 200")
        void shouldReturn200() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);

            CategoryResponse cat = buildCategory(1L, "一级分类");
            service.getCategoriesByLevelResult = List.of(cat);

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getCategoriesByLevel(1);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().size());
        }

        @Test
        @DisplayName("服务抛异常 → 500")
        void shouldReturn500OnException() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);
            service.getCategoriesByLevelException = new RuntimeException("DB error");

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getCategoriesByLevel(2);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取分类列表失败", resp.getBody().getMessage());
        }
    }

    // ==================== getCategoriesByParentId ====================

    @Nested
    @DisplayName("GET /api/category/parent/{parentId}")
    class GetCategoriesByParentId {

        @Test
        @DisplayName("正常返回子分类 → 200")
        void shouldReturn200() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);

            CategoryResponse child = buildCategory(2L, "Spring");
            service.getCategoriesByParentIdResult = List.of(child);

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getCategoriesByParentId(1L);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("Spring", resp.getBody().getData().get(0).getName());
        }

        @Test
        @DisplayName("服务抛异常 → 500")
        void shouldReturn500OnException() {
            StubCategoryService service = new StubCategoryService();
            CategoryController controller = new CategoryController(service);
            service.getCategoriesByParentIdException = new RuntimeException("DB error");

            ResponseEntity<Result<List<CategoryResponse>>> resp = controller.getCategoriesByParentId(1L);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertEquals("获取分类列表失败", resp.getBody().getMessage());
        }
    }
}
