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
import com.bookcollection.mapper.SysArticleMapper;
import com.bookcollection.mapper.SysCommentLikeMapper;
import com.bookcollection.mapper.SysCommentMapper;
import com.bookcollection.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final SysCommentMapper commentMapper;
    private final SysCommentLikeMapper commentLikeMapper;
    private final SysUserMapper userMapper;
    private final SysArticleMapper articleMapper;

    public CommentService(SysCommentMapper commentMapper, SysCommentLikeMapper commentLikeMapper,
                          SysUserMapper userMapper, SysArticleMapper articleMapper) {
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.userMapper = userMapper;
        this.articleMapper = articleMapper;
    }

    public Map<String, Object> getCommentList(Long articleId, Integer pageNum, Integer pageSize, Long userId) {
        LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysComment::getArticleId, articleId)
                .eq(SysComment::getStatus, 1)
                .orderByDesc(SysComment::getLikeCount)
                .orderByDesc(SysComment::getCreateTime);

        Page<SysComment> page = new Page<>(pageNum, pageSize);
        Page<SysComment> result = commentMapper.selectPage(page, wrapper);

        List<CommentResponse> comments = result.getRecords().stream()
                .map(comment -> toResponse(comment, userId))
                .collect(Collectors.toList());

        return Map.of(
                "list", comments,
                "total", result.getTotal(),
                "pageNum", result.getCurrent(),
                "pageSize", result.getSize(),
                "pages", result.getPages()
        );
    }

    public List<CommentResponse> getTopComments(Long articleId, Integer limit, Long userId) {
        LambdaQueryWrapper<SysComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysComment::getArticleId, articleId)
                .eq(SysComment::getStatus, 1)
                .orderByDesc(SysComment::getLikeCount)
                .orderByDesc(SysComment::getCreateTime)
                .last("LIMIT " + (limit != null ? limit : 2));

        List<SysComment> comments = commentMapper.selectList(wrapper);
        return comments.stream()
                .map(comment -> toResponse(comment, userId))
                .collect(Collectors.toList());
    }

    public CommentResponse createComment(CommentCreateRequest request, Long userId) {
        SysArticle article = articleMapper.selectById(request.getArticleId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        SysComment comment = new SysComment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setStatus(1);

        commentMapper.insert(comment);

        LambdaUpdateWrapper<SysArticle> articleWrapper = new LambdaUpdateWrapper<>();
        articleWrapper.eq(SysArticle::getId, request.getArticleId())
                .setSql("comment_count = comment_count + 1");
        articleMapper.update(null, articleWrapper);

        return toResponse(comment, userId);
    }

    public void deleteComment(Long commentId, Long userId) {
        SysComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        if (!comment.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此评论");
        }

        LambdaQueryWrapper<SysCommentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(SysCommentLike::getCommentId, commentId);
        commentLikeMapper.delete(likeWrapper);

        commentMapper.deleteById(commentId);

        LambdaUpdateWrapper<SysArticle> articleWrapper = new LambdaUpdateWrapper<>();
        articleWrapper.eq(SysArticle::getId, comment.getArticleId())
                .setSql("comment_count = comment_count - 1");
        articleMapper.update(null, articleWrapper);
    }

    public void likeComment(Long commentId, Long userId) {
        SysComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        LambdaQueryWrapper<SysCommentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(SysCommentLike::getCommentId, commentId)
                .eq(SysCommentLike::getUserId, userId);
        SysCommentLike existingLike = commentLikeMapper.selectOne(likeWrapper);

        if (existingLike != null) {
            throw new RuntimeException("已经点赞过了");
        }

        SysCommentLike commentLike = new SysCommentLike();
        commentLike.setCommentId(commentId);
        commentLike.setUserId(userId);
        commentLikeMapper.insert(commentLike);

        LambdaUpdateWrapper<SysComment> commentWrapper = new LambdaUpdateWrapper<>();
        commentWrapper.eq(SysComment::getId, commentId)
                .setSql("like_count = like_count + 1");
        commentMapper.update(null, commentWrapper);
    }

    public void unlikeComment(Long commentId, Long userId) {
        SysComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }

        LambdaQueryWrapper<SysCommentLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(SysCommentLike::getCommentId, commentId)
                .eq(SysCommentLike::getUserId, userId);
        SysCommentLike existingLike = commentLikeMapper.selectOne(likeWrapper);

        if (existingLike == null) {
            throw new RuntimeException("未点赞过此评论");
        }

        commentLikeMapper.deleteById(existingLike.getId());

        LambdaUpdateWrapper<SysComment> commentWrapper = new LambdaUpdateWrapper<>();
        commentWrapper.eq(SysComment::getId, commentId)
                .setSql("like_count = like_count - 1");
        commentMapper.update(null, commentWrapper);
    }

    private CommentResponse toResponse(SysComment comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setArticleId(comment.getArticleId());
        response.setUserId(comment.getUserId());
        response.setContent(comment.getContent());
        response.setLikeCount(comment.getLikeCount());
        response.setReplyCount(comment.getReplyCount());
        response.setStatus(comment.getStatus());
        response.setCreateTime(comment.getCreateTime());
        response.setUpdateTime(comment.getUpdateTime());

        SysUser user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            response.setUserName(user.getNickname());
            response.setUserAvatar(user.getAvatar());
        }

        if (currentUserId != null) {
            LambdaQueryWrapper<SysCommentLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(SysCommentLike::getCommentId, comment.getId())
                    .eq(SysCommentLike::getUserId, currentUserId);
            SysCommentLike like = commentLikeMapper.selectOne(likeWrapper);
            response.setIsLiked(like != null);
        } else {
            response.setIsLiked(false);
        }

        return response;
    }
}
