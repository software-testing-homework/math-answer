package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookcollection.dto.LoginRequest;
import com.bookcollection.dto.RegisterRequest;
import com.bookcollection.dto.UserResponse;
import com.bookcollection.entity.SysUser;
import com.bookcollection.mapper.SysUserMapper;
import com.bookcollection.utils.JwtUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final SysUserMapper userMapper;

    public UserService(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserResponse register(RegisterRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername())
                .or()
                .eq(request.getEmail() != null, SysUser::getEmail, request.getEmail())
                .or()
                .eq(request.getPhone() != null, SysUser::getPhone, request.getPhone());

        if (userMapper.selectCount(wrapper) > 0) {
            throw new RuntimeException("用户名、邮箱或手机号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(DigestUtils.md5DigestAsHex(request.getPassword().getBytes()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getUsername());
        user.setRole(0);
        user.setStatus(1);
        user.setLoginCount(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setToken(JwtUtils.generateToken(user.getId(), user.getUsername()));
        return response;
    }

    public UserResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new RuntimeException("账户已被禁用");
        }

        String password = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLoginCount(user.getLoginCount() + 1);
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setBio(user.getBio());
        response.setRole(user.getRole());
        response.setToken(JwtUtils.generateToken(user.getId(), user.getUsername()));
        return response;
    }

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }

    public SysUser updateProfile(Long userId, SysUser updateData) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (updateData.getNickname() != null) {
            if (updateData.getNickname().length() > 20) {
                throw new RuntimeException("昵称长度不能超过20个字符");
            }
            user.setNickname(updateData.getNickname());
        }

        if (updateData.getEmail() != null) {
            if (!updateData.getEmail().matches("^[\\w.-]+@[\\w.-]+\\.[\\w.-]+$")) {
                throw new RuntimeException("邮箱格式不正确");
            }
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getEmail, updateData.getEmail())
                    .ne(SysUser::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(updateData.getEmail());
        }

        if (updateData.getPhone() != null) {
            if (updateData.getPhone().length() > 20) {
                throw new RuntimeException("手机号长度不正确");
            }
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getPhone, updateData.getPhone())
                    .ne(SysUser::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new RuntimeException("手机号已被使用");
            }
            user.setPhone(updateData.getPhone());
        }

        if (updateData.getBio() != null) {
            if (updateData.getBio().length() > 500) {
                throw new RuntimeException("个人简介不能超过500个字符");
            }
            user.setBio(updateData.getBio());
        }

        if (updateData.getAvatar() != null) {
            if (updateData.getAvatar().length() > 256) {
                throw new RuntimeException("头像URL过长");
            }
            user.setAvatar(updateData.getAvatar());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return user;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        String oldPasswordMd5 = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!oldPasswordMd5.equals(user.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }

        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度应为6-20位");
        }

        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与原密码相同");
        }

        String newPasswordMd5 = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPassword(newPasswordMd5);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }
}
