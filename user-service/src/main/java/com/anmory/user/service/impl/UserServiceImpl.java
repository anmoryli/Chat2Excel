package com.anmory.user.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.anmory.common.util.JwtUtil;
import com.anmory.user.dto.request.AuthRequest;
import com.anmory.user.dto.request.ChangePasswordRequest;
import com.anmory.user.dto.response.AuthResponse;
import com.anmory.user.dto.response.ChangePasswordResponse;
import com.anmory.user.dto.response.UserInfoResponse;
import com.anmory.user.entity.UserEntity;
import com.anmory.user.mapper.UserMapper;
import com.anmory.user.service.UserService;
import com.anmory.user.service.VerificationCodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户服务实现类
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse auth(AuthRequest request) {
        // 前置校验：防止request为空
        if (request == null) {
            throw new IllegalArgumentException("认证请求参数不能为空");
        }

        // 1. 检测登录方式
        boolean isEmailCodeMode = StringUtils.isNotBlank(request.getEmail()) && StringUtils.isNotBlank(request.getVerificationCode());
        boolean isPasswordMode = StringUtils.isNotBlank(request.getUsername()) && StringUtils.isNotBlank(request.getPassword());

        // 校验登录方式：不能同时传两种，也不能都不传
        if (isEmailCodeMode && isPasswordMode) {
            log.error("[用户认证失败], 登录方式错误");
            throw new IllegalArgumentException("请提供有效的验证方式：邮箱+验证码/账号+密码");
        }
        if (!isEmailCodeMode && !isPasswordMode) {
            log.error("[用户认证失败], 未提供有效登录参数");
            throw new IllegalArgumentException("请提供邮箱+验证码 或 账号+密码进行认证");
        }

        UserEntity user = null;
        Boolean isNewUser = false;

        // 2. 处理邮箱+验证码登录
        if (isEmailCodeMode) {
            if (!verificationCodeService.verifyCode(request.getEmail(), request.getVerificationCode())) {
                log.error("[用户认证失败], 验证码错误，邮箱：{}", request.getEmail());
                throw new IllegalArgumentException("验证码无效或过期");
            }

            Long userIdFromCode = verificationCodeService.getUserId(request.getVerificationCode());

            if (userIdFromCode == 0L) {
                // 新用户注册
                log.info("[新用户注册], 邮箱：{}", request.getEmail());
                user = new UserEntity();
                user.setEmail(request.getEmail());
                user.setUserName(createRandomName());

                // 插入数据库
                userMapper.insert(user);

                // 强制查询 ID
                user = userMapper.findByLoginKey(user.getUserName());
                if (user == null) {
                    throw new IllegalArgumentException("新用户注册失败，ID生成异常");
                }

                isNewUser = true;
            } else {
                // 老用户查询
                user = userMapper.selectById(userIdFromCode);
                if (user == null) {
                    throw new IllegalArgumentException("验证码对应的用户不存在");
                }
            }

            // 删除已使用的验证码
            verificationCodeService.remove(request.getVerificationCode());
        }

        // 3. 处理账号密码登录
        if (isPasswordMode) {
            user = userMapper.findByLoginKey(request.getUsername());

            if (user == null) {
                // 新用户注册
                log.info("[新用户注册], 用户名：{}", request.getUsername());
                user = new UserEntity();
                user.setUserName(request.getUsername());
                user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

                // 插入数据库
                userMapper.insert(user);

                // 核心修复：强制查询 ID
                user = userMapper.findByLoginKey(request.getUsername());
                if (user == null) {
                    throw new IllegalArgumentException("新用户注册成功，但查询不到用户信息");
                }

                isNewUser = true;
            } else {
                // 老用户校验密码
                if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    throw new IllegalArgumentException("用户名与密码不匹配");
                }
            }
        }

        // 最终兜底：确保user和ID都不为空
        if (user == null) {
            throw new IllegalArgumentException("用户信息获取失败");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("用户ID获取失败，无法生成token");
        }

        // 4. 生成/获取Token
        String token;
        try {
            if (jwtUtil.isUserLogged(user.getId())) {
                token = jwtUtil.getUserActiveToken(user.getId());
            } else {
                token = jwtUtil.createToken(user.getId(), user.getUserName());
            }
        } catch (Exception e) {
            log.error("生成token失败", e);
            throw new IllegalArgumentException("登录成功，但生成token失败：" + e.getMessage());
        }

        // 5. 构建返回结果
        LocalDateTime expireTime = LocalDateTime.now().plusHours(20);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss");
        String tokenExpireTime = expireTime.format(formatter);

        return AuthResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .token(token)
                .tokenExpireTime(tokenExpireTime)
                .isNewUser(isNewUser)
                .build();
    }

    @Override
    public UserInfoResponse getUserInfo(String authorization) {
        // 1. 去jwt工具类里面查询userId
        Long userId = jwtUtil.getUserIdByAuthorization(authorization);
        if(userId == null) {
            throw new IllegalArgumentException("无效的令牌");
        }

        UserEntity user = userMapper.selectById(userId);
        if(user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return UserInfoResponse.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChangePasswordResponse changePassword(ChangePasswordRequest request, String authorization) {
        // 1. 新密码与确认密码必须一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("请保证两次密码一致");
        }

        // 2. 新密码与旧密码不能一致
        if (passwordEncoder.matches(request.getNewPassword(), userMapper.selectById(jwtUtil.getUserIdByAuthorization(authorization)).getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与旧密码一致");
        }

        // 3. 根据token获取用户信息
        Long userId = jwtUtil.getUserIdByAuthorization(authorization);
        if(userId == null) {
            throw new IllegalArgumentException("无效的令牌");
        }

        // 4. 获取用户信息之后进行修改
        UserEntity user = userMapper.selectById(userId);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);

        // 5. 构建返回结果
        return ChangePasswordResponse.builder()
                .userId(user.getId())
                .username(user.getUserName())
                .success(true)
                .build();
    }

    @Override
    public void logout(String authorization) {
        jwtUtil.removeAuthorization(authorization);
    }

    /**
     * 生成全局唯一的随机用户名
     */
    private String createRandomName() {
        String name;
        // 循环确保用户名唯一
        do {
            name = "chat2excel_" + String.valueOf((int) (Math.random() * 900000000) + 100000000);
        } while (userMapper.existByUserName(name) > 0);
        return name;
    }

    /**
     * 邮箱脱敏（备用方法）
     */
    private String maskEmail(String email) {
        if (StringUtils.isNotBlank(email)) {
            String[] parts = email.split("@", 2);
            String name = parts[0];
            String domain = parts[1];
            if (name.length() <= 2) {
                return name.charAt(0) + "***@" + domain;
            }
            return name.substring(0, 2) + "***@" + domain;
        }
        return email;
    }
}