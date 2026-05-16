package com.bookcollection.controller;

import com.bookcollection.dto.CategoryResponse;
import com.bookcollection.dto.Result;
import com.bookcollection.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public ResponseEntity<Result<List<CategoryResponse>>> getAllCategories() {
        try {
            List<CategoryResponse> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(Result.success(categories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类列表失败"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        try {
            CategoryResponse category = categoryService.getCategoryById(id);
            return ResponseEntity.ok(Result.success(category));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类详情失败"));
        }
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<Result<List<CategoryResponse>>> getCategoriesByLevel(@PathVariable Integer level) {
        try {
            List<CategoryResponse> categories = categoryService.getCategoriesByLevel(level);
            return ResponseEntity.ok(Result.success(categories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类列表失败"));
        }
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<Result<List<CategoryResponse>>> getCategoriesByParentId(@PathVariable Long parentId) {
        try {
            List<CategoryResponse> categories = categoryService.getCategoriesByParentId(parentId);
            return ResponseEntity.ok(Result.success(categories));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Result.error("获取分类列表失败"));
        }
    }
}
