package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookcollection.dto.*;
import com.bookcollection.entity.SysArticle;
import com.bookcollection.entity.SysCategory;
import com.bookcollection.entity.SysUser;
import com.bookcollection.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ArticleService {

    private final SysArticleMapper articleMapper;
    private final SysCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;
    private final SysCommentMapper commentMapper;
    private final SysCommentLikeMapper commentLikeMapper;

    public ArticleService(SysArticleMapper articleMapper, SysCategoryMapper categoryMapper, SysUserMapper userMapper,
                          SysCommentMapper commentMapper, SysCommentLikeMapper commentLikeMapper) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
    }

    public Map<String, Object> getArticleList(Integer pageNum, Integer pageSize, Long categoryId, String keyword, Integer status) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();

        if (categoryId != null) {
            wrapper.eq(SysArticle::getCategoryId, categoryId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like(SysArticle::getTitle, keyword)
                    .or()
                    .like(SysArticle::getSummary, keyword));
        }

        if (status != null) {
            wrapper.eq(SysArticle::getStatus, status);
        } else {
            wrapper.eq(SysArticle::getStatus, 1);
        }

        wrapper.orderByDesc(SysArticle::getIsTop)
                .orderByDesc(SysArticle::getPublishTime)
                .orderByDesc(SysArticle::getId);

        Page<SysArticle> page = new Page<>(pageNum, pageSize);
        Page<SysArticle> result = articleMapper.selectPage(page, wrapper);

        List<ArticleResponse> articles = result.getRecords().stream()
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

    public Map<String, Object> getUserArticles(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(SysArticle::getAuthorId, userId);

        if (status != null) {
            wrapper.eq(SysArticle::getStatus, status);
        }

        wrapper.orderByDesc(SysArticle::getPublishTime)
                .orderByDesc(SysArticle::getId);

        Page<SysArticle> page = new Page<>(pageNum, pageSize);
        Page<SysArticle> result = articleMapper.selectPage(page, wrapper);

        List<ArticleResponse> articles = result.getRecords().stream()
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

    public ArticleResponse getArticleById(Long id) {
        SysArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        increaseViewCount(id);

        return toResponse(article);
    }

    public ArticleResponse createArticle(ArticleCreateRequest request, Long authorId) {
        SysArticle article = new SysArticle();
        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setContentType(request.getContentType() != null ? request.getContentType() : 1);
        article.setCoverImage(request.getCoverImage());
        article.setCategoryId(request.getCategoryId());
        article.setAuthorId(authorId);
        article.setTags(request.getTags());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCommentCount(0);
        article.setStatus(1);
        article.setIsTop(0);
        article.setPublishTime(LocalDateTime.now());

        articleMapper.insert(article);

        return toResponse(article);
    }

    public ArticleResponse updateArticle(ArticleUpdateRequest request, Long userId) {
        SysArticle article = articleMapper.selectById(request.getId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权修改此文章");
        }

        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        if (request.getContentType() != null) {
            article.setContentType(request.getContentType());
        }
        if (request.getCoverImage() != null) {
            article.setCoverImage(request.getCoverImage());
        }
        if (request.getCategoryId() != null) {
            article.setCategoryId(request.getCategoryId());
        }
        if (request.getTags() != null) {
            article.setTags(request.getTags());
        }

        articleMapper.updateById(article);

        return toResponse(article);
    }

    public void deleteArticle(Long id, Long userId) {
        SysArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除此文章");
        }

        LambdaQueryWrapper<com.bookcollection.entity.SysComment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(com.bookcollection.entity.SysComment::getArticleId, id);
        List<com.bookcollection.entity.SysComment> comments = commentMapper.selectList(commentWrapper);

        for (com.bookcollection.entity.SysComment comment : comments) {
            LambdaQueryWrapper<com.bookcollection.entity.SysCommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(com.bookcollection.entity.SysCommentLike::getCommentId, comment.getId());
            commentLikeMapper.delete(likeWrapper);
        }

        LambdaQueryWrapper<com.bookcollection.entity.SysComment> deleteCommentWrapper = new LambdaQueryWrapper<>();
        deleteCommentWrapper.eq(com.bookcollection.entity.SysComment::getArticleId, id);
        commentMapper.delete(deleteCommentWrapper);

        articleMapper.deleteById(id);
    }

    public void increaseViewCount(Long id) {
        LambdaUpdateWrapper<SysArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysArticle::getId, id)
                .setSql("view_count = view_count + 1");
        articleMapper.update(null, wrapper);
    }

    public void increaseLikeCount(Long id) {
        LambdaUpdateWrapper<SysArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysArticle::getId, id)
                .setSql("like_count = like_count + 1");
        articleMapper.update(null, wrapper);
    }

    public void decreaseLikeCount(Long id) {
        LambdaUpdateWrapper<SysArticle> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysArticle::getId, id)
                .setSql("like_count = like_count - 1");
        articleMapper.update(null, wrapper);
    }

    public List<ArticleResponse> getHotArticles(Integer limit) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArticle::getStatus, 1)
                .orderByDesc(SysArticle::getViewCount)
                .orderByDesc(SysArticle::getLikeCount)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<SysArticle> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ArticleResponse> getHotArticlesByCategory(Long categoryId, Integer limit) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArticle::getStatus, 1)
                .eq(SysArticle::getCategoryId, categoryId)
                .orderByDesc(SysArticle::getViewCount)
                .orderByDesc(SysArticle::getLikeCount)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<SysArticle> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ArticleResponse> getLatestArticles(Integer limit) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArticle::getStatus, 1)
                .orderByDesc(SysArticle::getPublishTime)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<SysArticle> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ArticleResponse> getLatestArticlesByCategory(Long categoryId, Integer limit) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysArticle::getStatus, 1)
                .eq(SysArticle::getCategoryId, categoryId)
                .orderByDesc(SysArticle::getPublishTime)
                .last("LIMIT " + (limit != null ? limit : 10));

        List<SysArticle> articles = articleMapper.selectList(wrapper);
        return articles.stream().map(this::toResponse).collect(Collectors.toList());
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
