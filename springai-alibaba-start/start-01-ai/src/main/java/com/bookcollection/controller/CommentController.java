package com.bookcollection.controller;

import com.bookcollection.dto.CommentCreateRequest;
import com.bookcollection.dto.CommentResponse;
import com.bookcollection.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/list")
    public ResponseEntity<com.bookcollection.dto.Result<Map<String, Object>>> getCommentList(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                String tokenStr = token.startsWith("Bearer ") ? token.substring(7) : token;
                userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            }

            Map<String, Object> result = commentService.getCommentList(articleId, pageNum, pageSize, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("获取评论列表失败"));
        }
    }

    @GetMapping("/top")
    public ResponseEntity<com.bookcollection.dto.Result<List<CommentResponse>>> getTopComments(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "2") Integer limit,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = null;
            if (token != null && !token.isEmpty()) {
                String tokenStr = token.startsWith("Bearer ") ? token.substring(7) : token;
                userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            }

            List<CommentResponse> comments = commentService.getTopComments(articleId, limit, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success(comments));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("获取热门评论失败"));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<com.bookcollection.dto.Result<CommentResponse>> createComment(
            @RequestBody CommentCreateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            CommentResponse comment = commentService.createComment(request, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success(comment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(com.bookcollection.dto.Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("创建评论失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<com.bookcollection.dto.Result<String>> deleteComment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            commentService.deleteComment(id, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(com.bookcollection.dto.Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("删除评论失败"));
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<com.bookcollection.dto.Result<String>> likeComment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            commentService.likeComment(id, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success("点赞成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(com.bookcollection.dto.Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("点赞失败"));
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<com.bookcollection.dto.Result<String>> unlikeComment(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            commentService.unlikeComment(id, userId);
            return ResponseEntity.ok(com.bookcollection.dto.Result.success("取消点赞成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(com.bookcollection.dto.Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(com.bookcollection.dto.Result.error("取消点赞失败"));
        }
    }
}
