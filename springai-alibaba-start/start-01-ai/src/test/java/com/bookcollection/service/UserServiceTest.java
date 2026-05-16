package com.bookcollection.service;

import com.bookcollection.entity.SysUser;
import com.bookcollection.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private static final class MapperStub {
        private final SysUserMapper mapper;
        private SysUser selectByIdResult;
        private long selectCountResult;
        private final AtomicInteger updateByIdCalls = new AtomicInteger(0);
        private SysUser lastUpdatedUser;

        private MapperStub() {
            this.mapper = (SysUserMapper) Proxy.newProxyInstance(
                    SysUserMapper.class.getClassLoader(),
                    new Class<?>[]{SysUserMapper.class},
                    (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "selectById" -> selectByIdResult;
                            case "selectCount" -> selectCountResult;
                            case "updateById" -> {
                                updateByIdCalls.incrementAndGet();
                                lastUpdatedUser = (SysUser) args[0];
                                yield 1;
                            }
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    }
            );
        }
    }

    @Test
    void updateProfile_shouldThrowWhenUserNotFound() {
        MapperStub stub = new MapperStub();
        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    void updateProfile_shouldValidateNicknameLength() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setNickname("old");
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setNickname("123456789012345678901");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("昵称长度不能超过20个字符", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldValidateEmailFormat() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setEmail("not-an-email");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("邮箱格式不正确", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldThrowWhenEmailAlreadyUsed() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;
        stub.selectCountResult = 1;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setEmail("a@b.com");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("邮箱已被使用", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldValidatePhoneLength() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setPhone("123456789012345678901");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("手机号长度不正确", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldThrowWhenPhoneAlreadyUsed() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;
        stub.selectCountResult = 1;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setPhone("13800138000");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("手机号已被使用", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldValidateBioLength() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setBio("a".repeat(501));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("个人简介不能超过500个字符", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldValidateAvatarLength() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setAvatar("a".repeat(257));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateProfile(1L, update));
        assertEquals("头像URL过长", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void updateProfile_shouldUpdateFieldsAndCallMapper() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setNickname("old");
        existing.setUpdateTime(LocalDateTime.of(2000, 1, 1, 0, 0));
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        SysUser update = new SysUser();
        update.setNickname("newNick");
        update.setBio("bio");

        SysUser result = service.updateProfile(1L, update);

        assertEquals(1, stub.updateByIdCalls.get());
        assertSame(existing, result);
        assertEquals("newNick", result.getNickname());
        assertEquals("bio", result.getBio());
        assertNotNull(result.getUpdateTime());
    }

    @Test
    void changePassword_shouldThrowWhenUserNotFound() {
        MapperStub stub = new MapperStub();
        UserService service = new UserService(stub.mapper);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.changePassword(1L, "old", "new123"));
        assertEquals("用户不存在", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void changePassword_shouldValidateOldPassword() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setPassword(md5("right"));
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.changePassword(1L, "wrong", "new123"));
        assertEquals("原密码不正确", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void changePassword_shouldValidateNewPasswordLength() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setPassword(md5("old123"));
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.changePassword(1L, "old123", "123"));
        assertEquals("新密码长度应为6-20位", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void changePassword_shouldRejectSamePassword() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setPassword(md5("same123"));
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.changePassword(1L, "same123", "same123"));
        assertEquals("新密码不能与原密码相同", ex.getMessage());
        assertEquals(0, stub.updateByIdCalls.get());
    }

    @Test
    void changePassword_shouldUpdatePasswordAndCallMapper() {
        MapperStub stub = new MapperStub();
        SysUser existing = new SysUser();
        existing.setId(1L);
        existing.setPassword(md5("old123"));
        stub.selectByIdResult = existing;

        UserService service = new UserService(stub.mapper);

        service.changePassword(1L, "old123", "new123");

        assertEquals(1, stub.updateByIdCalls.get());
        assertEquals(md5("new123"), stub.lastUpdatedUser.getPassword());
        assertNotNull(stub.lastUpdatedUser.getUpdateTime());
    }

    private static String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }
}
