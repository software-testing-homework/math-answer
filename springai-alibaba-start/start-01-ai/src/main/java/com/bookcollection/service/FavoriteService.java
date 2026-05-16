package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookcollection.dto.ArticleResponse;
import com.bookcollection.entity.SysArticle;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.entity.SysUser;
import com.bookcollection.entity.UserFavorite;
import com.bookcollection.mapper.SysArticleMapper;
import com.bookcollection.mapper.SysCategoryMapper;
import com.bookcollection.mapper.SysUserMapper;
import com.bookcollection.mapper.UserFavoriteMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final UserFavoriteMapper favoriteMapper;
    private final SysArticleMapper articleMapper;
    private final SysCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    public FavoriteService(UserFavoriteMapper favoriteMapper, SysArticleMapper articleMapper,
                           SysCategoryMapper categoryMapper, SysUserMapper userMapper) {
        this.favoriteMapper = favoriteMapper;
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public void addFavorite(Long userId, Long articleId) {
        SysArticle article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        UserFavorite existing = favoriteMapper.findByUserAndTarget(userId, 3, articleId);
        if (existing != null) {
            throw new RuntimeException("已收藏该文章");
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setTargetType(3);
        favorite.setTargetId(articleId);
        favorite.setFolderId(0L);

        favoriteMapper.insert(favorite);
    }

    public void removeFavorite(Long userId, Long articleId) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, 3)
                .eq(UserFavorite::getTargetId, articleId);

        favoriteMapper.delete(wrapper);
    }

    public boolean isFavorited(Long userId, Long articleId) {
        UserFavorite favorite = favoriteMapper.findByUserAndTarget(userId, 3, articleId);
        return favorite != null;
    }

    public Map<String, Object> getFavoriteArticles(Long userId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<UserFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, 3)
                .orderByDesc(UserFavorite::getCreateTime);

        Page<UserFavorite> page = new Page<>(pageNum, pageSize);
        Page<UserFavorite> result = favoriteMapper.selectPage(page, wrapper);

        List<Long> articleIds = result.getRecords().stream()
                .map(UserFavorite::getTargetId)
                .collect(Collectors.toList());

        List<ArticleResponse> articles = articleIds.stream()
                .map(articleMapper::selectById)
                .filter(article -> article != null)
                .map(this::toResponse)
                .collect(Collectors.toList());

        return Map.of(
                "list", articles,
                "total", result.getTotal(),
                "pageNum", result.getCurrent(),
                "pageSize", result.getSize(),
                "pages", result.getPages()
        );
    }

    private ArticleResponse toResponse(SysArticle article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setSummary(article.getSummary());
        response.setContent(article.getContent());
        response.setContentType(article.getContentType());
        response.setCoverImage(article.getCoverImage());
        response.setCategoryId(article.getCategoryId());
        response.setAuthorId(article.getAuthorId());
        response.setTags(article.getTags());
        response.setViewCount(article.getViewCount());
        response.setLikeCount(article.getLikeCount());
        response.setCommentCount(article.getCommentCount());
        response.setStatus(article.getStatus());
        response.setIsTop(article.getIsTop());
        response.setPublishTime(article.getPublishTime());
        response.setCreateTime(article.getCreateTime());

        SysCategory category = categoryMapper.selectById(article.getCategoryId());
        if (category != null) {
            response.setCategoryName(category.getName());
        }

        SysUser author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            response.setAuthorName(author.getNickname());
            response.setAuthorAvatar(author.getAvatar());
        }

        return response;
    }
}
