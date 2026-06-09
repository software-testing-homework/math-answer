package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookcollection.dto.ArticleResponse;
import com.bookcollection.entity.SysArticle;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.entity.SysUser;
import com.bookcollection.entity.UserFavorite;
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
@DisplayName("FavoriteService 单元测试")
class FavoriteServiceTest {

    @Mock
    private UserFavoriteMapper favoriteMapper;
    @Mock
    private SysArticleMapper articleMapper;
    @Mock
    private SysCategoryMapper categoryMapper;
    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private FavoriteService favoriteService;

    // ==================== 辅助方法 ====================

    private SysArticle buildArticle(Long id, Long categoryId, Long authorId) {
        SysArticle article = new SysArticle();
        article.setId(id);
        article.setTitle("测试文章");
        article.setSummary("摘要");
        article.setContent("内容");
        article.setContentType(1);
        article.setCategoryId(categoryId);
        article.setAuthorId(authorId);
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

    private void stubToResponseDeps(Long categoryId, Long authorId) {
        when(categoryMapper.selectById(categoryId)).thenReturn(buildCategory(categoryId, "专栏"));
        when(userMapper.selectById(authorId)).thenReturn(buildUser(authorId, "作者", "a.png"));
    }

    // ==================== 1. addFavorite ====================

    @Nested
    @DisplayName("addFavorite — 添加收藏")
    class AddFavorite {

        @Test
        @DisplayName("正常收藏文章")
        void shouldAddFavorite() {
            SysArticle article = buildArticle(1L, 10L, 100L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(favoriteMapper.findByUserAndTarget(200L, 3, 1L)).thenReturn(null);
            when(favoriteMapper.insert(any(UserFavorite.class))).thenReturn(1);

            assertDoesNotThrow(() -> favoriteService.addFavorite(200L, 1L));

            verify(favoriteMapper, times(1)).insert(any(UserFavorite.class));
        }

        @Test
        @DisplayName("文章不存在时抛出异常")
        void shouldThrowWhenArticleNotFound() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> favoriteService.addFavorite(200L, 999L));
            assertEquals("文章不存在", ex.getMessage());
            verify(favoriteMapper, never()).insert(any());
        }

        @Test
        @DisplayName("已收藏时抛出异常")
        void shouldThrowWhenAlreadyFavorited() {
            SysArticle article = buildArticle(1L, 10L, 100L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            when(favoriteMapper.findByUserAndTarget(200L, 3, 1L))
                    .thenReturn(new UserFavorite());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> favoriteService.addFavorite(200L, 1L));
            assertEquals("已收藏该文章", ex.getMessage());
            verify(favoriteMapper, never()).insert(any());
        }
    }

    // ==================== 2. removeFavorite ====================

    @Nested
    @DisplayName("removeFavorite — 取消收藏")
    class RemoveFavorite {

        @Test
        @DisplayName("正常取消收藏")
        void shouldRemoveFavorite() {
            when(favoriteMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

            assertDoesNotThrow(() -> favoriteService.removeFavorite(200L, 1L));

            verify(favoriteMapper, times(1)).delete(any(LambdaQueryWrapper.class));
        }
    }

    // ==================== 3. isFavorited ====================

    @Nested
    @DisplayName("isFavorited — 判断是否已收藏")
    class IsFavorited {

        @Test
        @DisplayName("已收藏返回 true")
        void shouldReturnTrueWhenFavorited() {
            when(favoriteMapper.findByUserAndTarget(200L, 3, 1L))
                    .thenReturn(new UserFavorite());

            assertTrue(favoriteService.isFavorited(200L, 1L));
        }

        @Test
        @DisplayName("未收藏返回 false")
        void shouldReturnFalseWhenNotFavorited() {
            when(favoriteMapper.findByUserAndTarget(200L, 3, 1L)).thenReturn(null);

            assertFalse(favoriteService.isFavorited(200L, 1L));
        }
    }

    // ==================== 4. getFavoriteArticles ====================

    @Nested
    @DisplayName("getFavoriteArticles — 分页查收藏文章")
    class GetFavoriteArticles {

        @Test
        @DisplayName("正常返回收藏文章列表")
        void shouldReturnFavoriteArticles() {
            UserFavorite fav = new UserFavorite();
            fav.setId(1L);
            fav.setUserId(200L);
            fav.setTargetType(3);
            fav.setTargetId(1L);
            fav.setCreateTime(LocalDateTime.now());

            Page<UserFavorite> page = new Page<>(1, 10);
            page.setRecords(List.of(fav));
            page.setTotal(1);
            page.setCurrent(1);
            page.setSize(10);
            page.setPages(1);

            when(favoriteMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            SysArticle article = buildArticle(1L, 10L, 100L);
            when(articleMapper.selectById(1L)).thenReturn(article);
            stubToResponseDeps(10L, 100L);

            Map<String, Object> result = favoriteService.getFavoriteArticles(200L, 1, 10);

            assertNotNull(result);
            assertEquals(1L, result.get("total"));

            @SuppressWarnings("unchecked")
            List<ArticleResponse> list = (List<ArticleResponse>) result.get("list");
            assertEquals(1, list.size());
            assertEquals("测试文章", list.get(0).getTitle());
            assertEquals("专栏", list.get(0).getCategoryName());
            assertEquals("作者", list.get(0).getAuthorName());
        }

        @Test
        @DisplayName("无收藏时返回空列表")
        void shouldReturnEmptyWhenNoFavorites() {
            Page<UserFavorite> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0);

            when(favoriteMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);

            Map<String, Object> result = favoriteService.getFavoriteArticles(200L, 1, 10);

            @SuppressWarnings("unchecked")
            List<ArticleResponse> list = (List<ArticleResponse>) result.get("list");
            assertTrue(list.isEmpty());
            assertEquals(0L, result.get("total"));
        }

        @Test
        @DisplayName("文章已删除时跳过 null")
        void shouldSkipDeletedArticles() {
            UserFavorite fav = new UserFavorite();
            fav.setTargetId(999L);
            fav.setCreateTime(LocalDateTime.now());

            Page<UserFavorite> page = new Page<>(1, 10);
            page.setRecords(List.of(fav));
            page.setTotal(1);

            when(favoriteMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(page);
            when(articleMapper.selectById(999L)).thenReturn(null);

            Map<String, Object> result = favoriteService.getFavoriteArticles(200L, 1, 10);

            @SuppressWarnings("unchecked")
            List<ArticleResponse> list = (List<ArticleResponse>) result.get("list");
            // 已删除的文章被 filter 过滤掉
            assertTrue(list.isEmpty());
        }
    }
}
