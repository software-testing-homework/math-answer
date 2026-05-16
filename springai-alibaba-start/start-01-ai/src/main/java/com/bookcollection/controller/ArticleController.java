package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private final ArticleService articleService;
    private final com.bookcollection.service.FavoriteService favoriteService;

    public ArticleController(ArticleService articleService, com.bookcollection.service.FavoriteService favoriteService) {
        this.articleService = articleService;
        this.favoriteService = favoriteService;
    }

    @GetMapping("/list")
    public ResponseEntity<Result<Map<String, Object>>> getArticleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        try {
            Map<String, Object> result = articleService.getArticleList(pageNum, pageSize, categoryId, keyword, status);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取文章列表失败"));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<Result<Map<String, Object>>> getUserArticles(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            Map<String, Object> result = articleService.getUserArticles(userId, pageNum, pageSize, status);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取我的文章列表失败"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<ArticleResponse>> getArticleById(@PathVariable Long id) {
        try {
            ArticleResponse article = articleService.getArticleById(id);
            return ResponseEntity.ok(Result.success(article));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取文章详情失败"));
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Result<ArticleResponse>> createArticle(
            @RequestBody ArticleCreateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            ArticleResponse article = articleService.createArticle(request, userId);
            return ResponseEntity.ok(Result.success(article));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("创建文章失败"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Result<ArticleResponse>> updateArticle(
            @RequestBody ArticleUpdateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            ArticleResponse article = articleService.updateArticle(request, userId);
            return ResponseEntity.ok(Result.success(article));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("更新文章失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<String>> deleteArticle(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            articleService.deleteArticle(id, userId);
            return ResponseEntity.ok(Result.success("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("删除文章失败"));
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Result<String>> increaseLikeCount(@PathVariable Long id) {
        try {
            articleService.increaseLikeCount(id);
            return ResponseEntity.ok(Result.success("点赞成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("点赞失败"));
        }
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Result<String>> decreaseLikeCount(@PathVariable Long id) {
        try {
            articleService.decreaseLikeCount(id);
            return ResponseEntity.ok(Result.success("取消点赞成功"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("取消点赞失败"));
        }
    }

    @GetMapping("/hot")
    public ResponseEntity<Result<List<ArticleResponse>>> getHotArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<ArticleResponse> articles = articleService.getHotArticles(limit);
            return ResponseEntity.ok(Result.success(articles));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取热门文章失败"));
        }
    }

    @GetMapping("/hot/category/{categoryId}")
    public ResponseEntity<Result<List<ArticleResponse>>> getHotArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<ArticleResponse> articles = articleService.getHotArticlesByCategory(categoryId, limit);
            return ResponseEntity.ok(Result.success(articles));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类热门文章失败"));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<Result<List<ArticleResponse>>> getLatestArticles(
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<ArticleResponse> articles = articleService.getLatestArticles(limit);
            return ResponseEntity.ok(Result.success(articles));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取最新文章失败"));
        }
    }

    @GetMapping("/latest/category/{categoryId}")
    public ResponseEntity<Result<List<ArticleResponse>>> getLatestArticlesByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<ArticleResponse> articles = articleService.getLatestArticlesByCategory(categoryId, limit);
            return ResponseEntity.ok(Result.success(articles));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类最新文章失败"));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadCover(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String token) {
        Map<String, Object> result = new HashMap<>();

        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("message", "请选择要上传的文件");
            return ResponseEntity.badRequest().body(result);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            result.put("code", 400);
            result.put("message", "文件大小不能超过5MB");
            return ResponseEntity.badRequest().body(result);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.put("code", 400);
            result.put("message", "只能上传图片文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            File uploadDir = new File(UPLOAD_DIR + "article/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFilename = "article_" + System.currentTimeMillis() + extension;
            Path filePath = Paths.get(UPLOAD_DIR + "article/" + newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/article/" + newFilename;

            result.put("code", 200);
            result.put("message", "上传成功");
            result.put("data", Map.of("url", fileUrl));

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "文件上传失败");
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/{id}/favorite/status")
    public ResponseEntity<Result<Boolean>> getFavoriteStatus(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || token.isEmpty()) {
                return ResponseEntity.ok(Result.success(false));
            }
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            boolean isFavorited = favoriteService.isFavorited(userId, id);
            return ResponseEntity.ok(Result.success(isFavorited));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.success(false));
        }
    }
}
