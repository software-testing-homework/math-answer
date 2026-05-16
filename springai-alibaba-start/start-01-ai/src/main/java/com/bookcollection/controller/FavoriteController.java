package com.bookcollection.controller;

import com.bookcollection.dto.Result;
import com.bookcollection.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/article/{articleId}")
    public ResponseEntity<Result<String>> addFavorite(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            favoriteService.addFavorite(userId, articleId);
            return ResponseEntity.ok(Result.success("收藏成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("收藏失败"));
        }
    }

    @DeleteMapping("/article/{articleId}")
    public ResponseEntity<Result<String>> removeFavorite(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            favoriteService.removeFavorite(userId, articleId);
            return ResponseEntity.ok(Result.success("取消收藏成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("取消收藏失败"));
        }
    }

    @GetMapping("/article/{articleId}/status")
    public ResponseEntity<Result<Boolean>> getFavoriteStatus(
            @PathVariable Long articleId,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            boolean isFavorited = favoriteService.isFavorited(userId, articleId);
            return ResponseEntity.ok(Result.success(isFavorited));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取收藏状态失败"));
        }
    }

    @GetMapping("/articles")
    public ResponseEntity<Result<Map<String, Object>>> getFavoriteArticles(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            Map<String, Object> result = favoriteService.getFavoriteArticles(userId, pageNum, pageSize);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取收藏列表失败"));
        }
    }
}
