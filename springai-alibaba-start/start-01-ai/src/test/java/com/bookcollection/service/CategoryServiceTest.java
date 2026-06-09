package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookcollection.dto.CategoryResponse;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.mapper.SysCategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService 单元测试")
class CategoryServiceTest {

    @Mock
    private SysCategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    // ==================== 辅助方法 ====================

    private SysCategory buildCategory(Long id, String name, Integer level, Long parentId) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setName(name);
        category.setCode("java");
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSort(1);
        category.setDescription("分类描述");
        category.setIcon("icon.png");
        category.setStatus(1);
        return category;
    }

    // ==================== 1. getAllCategories ====================

    @Nested
    @DisplayName("getAllCategories — 获取所有启用分类")
    class GetAllCategories {

        @Test
        @DisplayName("正常返回分类列表")
        void shouldReturnAllCategories() {
            SysCategory cat = buildCategory(1L, "Java分类", 1, 0L);
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(cat));

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Java分类", result.get(0).getName());
            assertEquals("java", result.get(0).getCode());
            verify(categoryMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无分类时返回空列表")
        void shouldReturnEmptyList() {
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<CategoryResponse> result = categoryService.getAllCategories();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 2. getCategoryById ====================

    @Nested
    @DisplayName("getCategoryById — 根据ID查分类")
    class GetCategoryById {

        @Test
        @DisplayName("分类存在时返回 CategoryResponse")
        void shouldReturnCategory() {
            SysCategory cat = buildCategory(1L, "Java分类", 1, 0L);
            when(categoryMapper.selectById(1L)).thenReturn(cat);

            CategoryResponse result = categoryService.getCategoryById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Java分类", result.getName());
            verify(categoryMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("分类不存在时抛出异常")
        void shouldThrowWhenNotFound() {
            when(categoryMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> categoryService.getCategoryById(999L));
            assertEquals("分类不存在", ex.getMessage());
        }
    }

    // ==================== 3. getCategoriesByLevel ====================

    @Nested
    @DisplayName("getCategoriesByLevel — 按层级查分类")
    class GetCategoriesByLevel {

        @Test
        @DisplayName("正常返回指定层级分类")
        void shouldReturnCategoriesByLevel() {
            SysCategory cat = buildCategory(1L, "Java分类", 1, 0L);
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(cat));

            List<CategoryResponse> result = categoryService.getCategoriesByLevel(1);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(1, result.get(0).getLevel());
            verify(categoryMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("该层级无分类时返回空列表")
        void shouldReturnEmptyWhenNoMatch() {
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<CategoryResponse> result = categoryService.getCategoriesByLevel(3);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 4. getCategoriesByParentId ====================

    @Nested
    @DisplayName("getCategoriesByParentId — 按父级ID查子分类")
    class GetCategoriesByParentId {

        @Test
        @DisplayName("正常返回子分类列表")
        void shouldReturnSubCategories() {
            SysCategory child = buildCategory(2L, "Spring Boot", 2, 1L);
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(child));

            List<CategoryResponse> result = categoryService.getCategoriesByParentId(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Spring Boot", result.get(0).getName());
            assertEquals(1L, result.get(0).getParentId());
            verify(categoryMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("无子分类时返回空列表")
        void shouldReturnEmptyWhenNoChildren() {
            when(categoryMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<CategoryResponse> result = categoryService.getCategoriesByParentId(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
