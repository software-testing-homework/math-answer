package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.entity.SysUser;
import com.bookcollection.service.UserService;
import com.bookcollection.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import com.bookcollection.mapper.SysUserMapper;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private static final class StubUserService extends UserService {
        private UserResponse registerResult;
        private RuntimeException registerException;

        private UserResponse loginResult;
        private RuntimeException loginException;

        private SysUser getUserByIdResult;

        private SysUser updateProfileResult;
        private RuntimeException updateProfileException;

        private RuntimeException changePasswordException;
        private Long lastChangePasswordUserId;
        private String lastChangePasswordOld;
        private String lastChangePasswordNew;

        private StubUserService() {
            super((SysUserMapper) null);
        }

        @Override
        public UserResponse register(RegisterRequest request) {
            if (registerException != null) {
                throw registerException;
            }
            return registerResult;
        }

        @Override
        public UserResponse login(LoginRequest request) {
            if (loginException != null) {
                throw loginException;
            }
            return loginResult;
        }

        @Override
        public SysUser getUserById(Long id) {
            return getUserByIdResult;
        }

        @Override
        public SysUser updateProfile(Long userId, SysUser updateData) {
            if (updateProfileException != null) {
                throw updateProfileException;
            }
            return updateProfileResult;
        }

        @Override
        public void changePassword(Long userId, String oldPassword, String newPassword) {
            this.lastChangePasswordUserId = userId;
            this.lastChangePasswordOld = oldPassword;
            this.lastChangePasswordNew = newPassword;
            if (changePasswordException != null) {
                throw changePasswordException;
            }
        }
    }

    @Test
    void register_shouldReturn200OnSuccess() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("p");

        UserResponse user = new UserResponse();
        user.setId(1L);
        user.setUsername("alice");

        userService.registerResult = user;

        ResponseEntity<Result<UserResponse>> response = controller.register(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("success", response.getBody().getMessage());
        assertEquals(user, response.getBody().getData());
    }

    @Test
    void register_shouldReturn400OnRuntimeException() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("p");

        userService.registerException = new RuntimeException("用户名已存在");

        ResponseEntity<Result<UserResponse>> response = controller.register(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("用户名已存在", response.getBody().getMessage());
    }

    @Test
    void login_shouldReturn401OnRuntimeException() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("bad");

        userService.loginException = new RuntimeException("账号或密码错误");

        ResponseEntity<Result<UserResponse>> response = controller.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("账号或密码错误", response.getBody().getMessage());
    }

    @Test
    void getProfile_shouldReturn200WhenUserExists() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        String token = JwtUtils.generateToken(99L, "alice");
        SysUser user = new SysUser();
        user.setId(99L);
        user.setUsername("alice");

        userService.getUserByIdResult = user;

        ResponseEntity<Result<SysUser>> response = controller.getProfile("Bearer " + token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(user, response.getBody().getData());
    }

    @Test
    void getProfile_shouldReturn404WhenUserNotFound() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        String token = JwtUtils.generateToken(99L, "alice");
        userService.getUserByIdResult = null;

        ResponseEntity<Result<SysUser>> response = controller.getProfile("Bearer " + token);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("用户不存在", response.getBody().getMessage());
    }

    @Test
    void getProfile_shouldReturn401WhenTokenInvalid() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        ResponseEntity<Result<SysUser>> response = controller.getProfile("Bearer not-a-jwt");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("无效的token", response.getBody().getMessage());
    }

    @Test
    void updateProfile_shouldReturn400OnRuntimeException() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        String token = JwtUtils.generateToken(5L, "alice");
        SysUser update = new SysUser();
        update.setNickname("n");

        userService.updateProfileException = new RuntimeException("参数错误");

        ResponseEntity<Result<SysUser>> response = controller.updateProfile("Bearer " + token, update);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getCode());
        assertEquals("参数错误", response.getBody().getMessage());
    }

    @Test
    void changePassword_shouldReturn200OnSuccess() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        String token = JwtUtils.generateToken(5L, "alice");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("new");

        ResponseEntity<Result<String>> response = controller.changePassword("Bearer " + token, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("密码修改成功", response.getBody().getData());
        assertEquals(5L, userService.lastChangePasswordUserId);
        assertEquals("old", userService.lastChangePasswordOld);
        assertEquals("new", userService.lastChangePasswordNew);
    }

    @Test
    void uploadAvatar_shouldRejectEmptyFile() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        ResponseEntity<java.util.Map<String, Object>> response = controller.uploadAvatar(file, "Bearer any");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("code"));
        assertEquals("请选择要上传的文件", response.getBody().get("message"));
    }

    @Test
    void uploadAvatar_shouldRejectNonImageContentType() {
        StubUserService userService = new StubUserService();
        UserController controller = new UserController(userService);

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "hi".getBytes());

        ResponseEntity<java.util.Map<String, Object>> response = controller.uploadAvatar(file, "Bearer any");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("code"));
        assertEquals("只能上传图片文件", response.getBody().get("message"));
    }
}
