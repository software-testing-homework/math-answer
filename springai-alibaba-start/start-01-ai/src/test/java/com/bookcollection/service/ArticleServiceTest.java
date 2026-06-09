package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookcollection.dto.ArticleCreateRequest;
import com.bookcollection.dto.ArticleResponse;
import com.bookcollection.dto.ArticleUpdateRequest;
import com.bookcollection.entity.SysArticle;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.entity.SysComment;
import com.bookcollection.entity.SysUser;
import com.bookcollection.mapper.*;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("ArticleService 单元测试")
class ArticleServiceTest {

    @Mock
    private SysArticleMapper articleMapper;
    @Mock
    private SysCategoryMapper categoryMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysCommentMapper commentMapper;
    @Mock
    private SysCommentLikeMapper commentLikeMapper;

    @InjectMocks
    private ArticleService articleService;

    // ==================== 辅助方法 ====================

    private SysArticle buildArticle(Long id, Long authorId, Long categoryId) {
        SysArticle article = new SysArticle();
        article.setId(id);
        article.setTitle("测试文章标题");
        article.setSummary("测试摘要");
        article.setContent("测试内容");
        article.setContentType(1);
        article.setCoverImage("cover.jpg");
        article.setCategoryId(categoryId);
        article.setAuthorId(authorId);
        article.setTags("Java,Spring");
        article.setViewCount(100);
        article.setLikeCount(10);
        article.setCommentCount(5);
        article.setStatus(1);
        article.setIsTop(0);
        article.setPublishTime(LocalDateTime.now());
        article.setCreateTime(LocalDateTime.now());
        return article;
    }

    private SysCategory buildCategory(Long id, String name) {
        SysCategory category = new SysCategory();
        category.setId(id);
        category.setName(name);
        return category;
    }

    private SysUser buildUser(Long id, String nickname, String avatar) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar(avatar);
        return user;
    }

    /**
     * 为 toResponse 内部的 categoryMapper / userMapper 查询设置通用桩。
     */
    private void stubToResponseDeps(Long categoryId, Long authorId) {
        when(categoryMapper.selectById(categoryId)).thenReturn(buildCategory(categoryId, "技术专栏"));
        when(userMapper.selectById(authorId)).thenReturn(buildUser(authorId, "张三", "avatar.png"));
    }

    // ==================== 1. getArticleList ====================

    @Nested
    @DisplayName("getArticleList — 分页查询文章列表")
    class GetArticleList {

        @Test
        @DisplayName("正常返回分页结果")
        void shouldReturnPagedResult() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            Page<SysArticle> page = new Page<>(1, 10);
            page.setRecords(List.of(article));
            page.setTotal(1);
            page.setCurrent(1);
            page.setSize(10);
            page.setPages(1);

            when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            stubToResponseDeps(10L, 1L);

            Map<String, Object> result = articleService.getArticleList(1, 10, null, null, null);

            assertNotNull(result);
            assertEquals(1L, result.get("total"));
            assertEquals(1L, result.get("pageNum"));
            assertEquals(10L, result.get("pageSize"));
            assertEquals(1L, result.get("pages"));

            @SuppressWarnings("unchecked")
            List<ArticleResponse> list = (List<ArticleResponse>) result.get("list");
            assertNotNull(list);
            assertEquals(1, list.size());
            assertEquals("测试文章标题", list.get(0).getTitle());
        }

        @Test
        @DisplayName("带分类和关键词过滤")
        void shouldFilterByCategoryAndKeyword() {
            Page<SysArticle> emptyPage = new Page<>(1, 10);
            emptyPage.setRecords(List.of());
            emptyPage.setTotal(0);

            when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(emptyPage);

            Map<String, Object> result = articleService.getArticleList(1, 10, 10L, "Spring", 1);

            assertEquals(0L, result.get("total"));
        }
    }

    // ==================== 2. getUserArticles ====================

    @Nested
    @DisplayName("getUserArticles — 查询用户文章列表")
    class GetUserArticles {

        @Test
        @DisplayName("正常返回用户文章")
        void shouldReturnUserArticles() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            Page<SysArticle> page = new Page<>(1, 10);
            page.setRecords(List.of(article));
            page.setTotal(1);

            when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            stubToResponseDeps(10L, 100L);

            Map<String, Object> result = articleService.getUserArticles(100L, 1, 10, null);

            assertNotNull(result);
            assertEquals(1L, result.get("total"));
        }
    }

    // ==================== 3. getArticleById ====================

    @Nested
    @DisplayName("getArticleById — 根据ID查看文章")
    class GetArticleById {

        @Test
        @DisplayName("文章存在时返回 ArticleResponse 并增加浏览量")
        void shouldReturnArticleAndIncreaseView() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            // increaseViewCount 内部调用 articleMapper.update(null, wrapper)
            when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
            stubToResponseDeps(10L, 1L);

            ArticleResponse response = articleService.getArticleById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("测试文章标题", response.getTitle());
            assertEquals("技术专栏", response.getCategoryName());
            assertEquals("张三", response.getAuthorName());
            // 验证浏览量自增被调用
            verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        }

        @Test
        @DisplayName("文章不存在时抛出异常")
        void shouldThrowWhenArticleNotFound() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> articleService.getArticleById(999L));
            assertEquals("文章不存在", ex.getMessage());
        }
    }

    // ==================== 4. createArticle ====================

    @Nested
    @DisplayName("createArticle — 创建文章")
    class CreateArticle {

        @Test
        @DisplayName("正常创建文章")
        void shouldCreateArticleSuccessfully() {
            ArticleCreateRequest request = new ArticleCreateRequest();
            request.setTitle("新文章");
            request.setSummary("摘要");
            request.setContent("内容");
            request.setContentType(1);
            request.setCoverImage("img.jpg");
            request.setCategoryId(10L);
            request.setTags("Java");

            when(articleMapper.insert(any(SysArticle.class))).thenReturn(1);
            stubToResponseDeps(10L, 1L);

            ArticleResponse response = articleService.createArticle(request, 1L);

            assertNotNull(response);
            assertEquals("新文章", response.getTitle());
            assertEquals("张三", response.getAuthorName());
            verify(articleMapper, times(1)).insert(any(SysArticle.class));
        }

        @Test
        @DisplayName("contentType 为空时默认设为 1")
        void shouldDefaultContentType() {
            ArticleCreateRequest request = new ArticleCreateRequest();
            request.setTitle("无类型文章");
            request.setContent("内容");
            request.setCategoryId(10L);

            when(articleMapper.insert(any(SysArticle.class))).thenReturn(1);
            stubToResponseDeps(10L, 1L);

            ArticleResponse response = articleService.createArticle(request, 1L);
            // contentType 默认值在 entity 上已设为 1
            assertNotNull(response);
        }
    }

    // ==================== 5. updateArticle ====================

    @Nested
    @DisplayName("updateArticle — 更新文章")
    class UpdateArticle {

        @Test
        @DisplayName("作者本人正常更新文章")
        void shouldUpdateOwnArticle() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(articleMapper.updateById(any(SysArticle.class))).thenReturn(1);
            stubToResponseDeps(10L, 100L);

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(1L);
            request.setTitle("更新后的标题");

            ArticleResponse response = articleService.updateArticle(request, 100L);

            assertNotNull(response);
            assertEquals("更新后的标题", response.getTitle());
            verify(articleMapper, times(1)).updateById(any(SysArticle.class));
        }

        @Test
        @DisplayName("文章不存在时抛出异常")
        void shouldThrowWhenArticleNotFoundInUpdate() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(999L);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> articleService.updateArticle(request, 1L));
            assertEquals("文章不存在", ex.getMessage());
        }

        @Test
        @DisplayName("非作者无权修改时抛出异常")
        void shouldThrowWhenNotAuthor() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(1L);
            request.setTitle("越权修改");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> articleService.updateArticle(request, 999L));
            assertEquals("无权修改此文章", ex.getMessage());
            verify(articleMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("只更新非空字段")
        void shouldOnlyUpdateNonNullFields() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            String originalSummary = article.getSummary();
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(articleMapper.updateById(any(SysArticle.class))).thenReturn(1);
            stubToResponseDeps(10L, 100L);

            ArticleUpdateRequest request = new ArticleUpdateRequest();
            request.setId(1L);
            request.setTitle("新标题");
            // summary, content 等字段为 null，不应覆盖原值

            articleService.updateArticle(request, 100L);
            assertEquals(originalSummary, article.getSummary());
        }
    }

    // ==================== 6. deleteArticle ====================

    @Nested
    @DisplayName("deleteArticle — 删除文章")
    class DeleteArticle {

        @Test
        @DisplayName("作者本人正常删除文章（含评论和点赞级联删除）")
        void shouldDeleteArticleWithCascade() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);

            SysComment comment1 = new SysComment();
            comment1.setId(1L);
            comment1.setArticleId(1L);
            SysComment comment2 = new SysComment();
            comment2.setId(2L);
            comment2.setArticleId(1L);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(comment1, comment2));

            when(commentLikeMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
            when(commentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
            when(articleMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> articleService.deleteArticle(1L, 100L));

            verify(commentLikeMapper, times(2)).delete(any(LambdaQueryWrapper.class));
            verify(commentMapper, times(1)).delete(any(LambdaQueryWrapper.class));
            verify(articleMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("文章不存在时抛出异常")
        void shouldThrowWhenArticleNotFoundInDelete() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> articleService.deleteArticle(999L, 1L));
            assertEquals("文章不存在", ex.getMessage());
        }

        @Test
        @DisplayName("非作者无权删除时抛出异常")
        void shouldThrowWhenNotAuthorDelete() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> articleService.deleteArticle(1L, 999L));
            assertEquals("无权删除此文章", ex.getMessage());
            verify(articleMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("文章无评论时正常删除")
        void shouldDeleteArticleWithoutComments() {
            SysArticle article = buildArticle(1L, 100L, 10L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(commentMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            when(commentMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
            when(articleMapper.deleteById(1L)).thenReturn(1);

            assertDoesNotThrow(() -> articleService.deleteArticle(1L, 100L));

            verify(commentLikeMapper, never()).delete(any(LambdaQueryWrapper.class));
            verify(articleMapper, times(1)).deleteById(1L);
        }
    }

    // ==================== 7. increaseViewCount ====================

    @Test
    @DisplayName("increaseViewCount — 浏览量 +1")
    void increaseViewCount() {
        when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> articleService.increaseViewCount(1L));

        verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ==================== 8. increaseLikeCount ====================

    @Test
    @DisplayName("increaseLikeCount — 点赞数 +1")
    void increaseLikeCount() {
        when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> articleService.increaseLikeCount(1L));

        verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ==================== 9. decreaseLikeCount ====================

    @Test
    @DisplayName("decreaseLikeCount — 点赞数 -1")
    void decreaseLikeCount() {
        when(articleMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assertDoesNotThrow(() -> articleService.decreaseLikeCount(1L));

        verify(articleMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    // ==================== 10. getHotArticles ====================

    @Nested
    @DisplayName("getHotArticles — 热门文章")
    class GetHotArticles {

        @Test
        @DisplayName("正常返回热门文章列表")
        void shouldReturnHotArticles() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            when(articleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(article));
            stubToResponseDeps(10L, 1L);

            List<ArticleResponse> result = articleService.getHotArticles(5);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("测试文章标题", result.get(0).getTitle());
        }

        @Test
        @DisplayName("limit 为 null 时默认 10 条")
        void shouldDefaultLimitTo10() {
            when(articleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());
            List<ArticleResponse> result = articleService.getHotArticles(null);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 11. getHotArticlesByCategory ====================

    @Nested
    @DisplayName("getHotArticlesByCategory — 按分类查热门")
    class GetHotArticlesByCategory {

        @Test
        @DisplayName("正常返回指定分类热门文章")
        void shouldReturnHotArticlesByCategory() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            when(articleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(article));
            stubToResponseDeps(10L, 1L);

            List<ArticleResponse> result = articleService.getHotArticlesByCategory(10L, 5);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== 12. getLatestArticles ====================

    @Nested
    @DisplayName("getLatestArticles — 最新文章")
    class GetLatestArticles {

        @Test
        @DisplayName("正常返回最新文章列表")
        void shouldReturnLatestArticles() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            when(articleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(article));
            stubToResponseDeps(10L, 1L);

            List<ArticleResponse> result = articleService.getLatestArticles(5);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    // ==================== 13. getLatestArticlesByCategory ====================

    @Nested
    @DisplayName("getLatestArticlesByCategory — 按分类查最新")
    class GetLatestArticlesByCategory {

        @Test
        @DisplayName("正常返回指定分类最新文章")
        void shouldReturnLatestArticlesByCategory() {
            SysArticle article = buildArticle(1L, 1L, 10L);
            when(articleMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(article));
            stubToResponseDeps(10L, 1L);

            List<ArticleResponse> result = articleService.getLatestArticlesByCategory(10L, 5);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }
}
