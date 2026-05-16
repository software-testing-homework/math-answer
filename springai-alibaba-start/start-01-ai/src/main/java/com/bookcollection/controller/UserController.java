package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.entity.SysUser;
import com.bookcollection.service.UserService;
import org.springframework.http.HttpStatus;
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
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<UserResponse>> register(@RequestBody RegisterRequest request) {
        try {
            UserResponse user = userService.register(request);
            return ResponseEntity.ok(Result.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Result<UserResponse>> login(@RequestBody LoginRequest request) {
        try {
            UserResponse user = userService.login(request);
            return ResponseEntity.ok(Result.success(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error(e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Result<SysUser>> getProfile(@RequestHeader("Authorization") String token) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            SysUser user = userService.getUserById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Result.error("用户不存在"));
            }
            return ResponseEntity.ok(Result.success(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error("无效的token"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<Result<SysUser>> updateProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody SysUser updateData) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            SysUser updatedUser = userService.updateProfile(userId, updateData);
            return ResponseEntity.ok(Result.success(updatedUser));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error("无效的token"));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
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
            result.put("message", "文件大小不能超过2MB");
            return ResponseEntity.badRequest().body(result);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            result.put("code", 400);
            result.put("message", "只能上传图片文件");
            return ResponseEntity.badRequest().body(result);
        }

        try {
            File uploadDir = new File(UPLOAD_DIR + "avatar/");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String newFilename = "avatar_" + System.currentTimeMillis() + extension;
            Path filePath = Paths.get(UPLOAD_DIR + "avatar/" + newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/avatar/" + newFilename;

            result.put("code", 200);
            result.put("message", "上传成功");
            result.put("data", Map.of("url", fileUrl));

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            result.put("code", 500);
            result.put("message", "文件上传失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PutMapping("/password")
    public ResponseEntity<Result<String>> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody ChangePasswordRequest request) {
        try {
            String tokenStr = token.substring(7);
            Long userId = com.bookcollection.utils.JwtUtils.getUserId(tokenStr);
            userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Result.success("密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Result.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.error("无效的token"));
        }
    }
}
