package com.anmory.user.controller;

import com.anmory.common.annotation.LogOperation;
import com.anmory.common.util.Result;
import com.anmory.user.dto.request.AuthRequest;
import com.anmory.user.dto.request.ChangePasswordRequest;
import com.anmory.user.dto.request.SendCodeRequest;
import com.anmory.user.dto.response.AuthResponse;
import com.anmory.user.dto.response.ChangePasswordResponse;
import com.anmory.user.dto.response.SendCodeResponse;
import com.anmory.user.service.UserService;
import com.anmory.user.service.VerificationCodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.anmory.user.dto.response.UserInfoResponse;

/**
 * 用户服务控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private VerificationCodeService verificationCodeService;
    @Autowired
    private UserService userService;
    /**
     * 发送邮箱验证码接口
     */
    @RequestMapping("/verification-code")
    @LogOperation("发送邮箱验证码")
    public Result<SendCodeResponse> sendVerificationCode(@RequestBody @Valid SendCodeRequest request) throws JsonProcessingException {
        int expireSeconds = verificationCodeService.sendCode(request.getEmail());
        String sendTo = request.getEmail();
        SendCodeResponse response = SendCodeResponse.builder()
                .sendTo(maskEmail(sendTo))
                .expireTime(expireSeconds)
                .build();
        return Result.success("验证码发送成功", response);
    }

    @PostMapping("/auth")
    @LogOperation("用户认证")
    public Result<AuthResponse> auth(@RequestBody @Valid AuthRequest request) {
        AuthResponse response = userService.auth(request);
        String message = response.getIsNewUser() ? "用户注册并登录成功" : "用户登录成功";
        return Result.success(message, response);
    }

    /**
     * 获取用户信息接口
     */
    @GetMapping("/info")
    @LogOperation("获取用户信息")
    public Result<UserInfoResponse> getUserInfo(@RequestHeader(value = "Authorization", required = true) String authorization) {
        return Result.success("success",userService.getUserInfo(authorization));
    }

    @PostMapping("change-password")
    @LogOperation("修改密码")
    public Result<ChangePasswordResponse> changePassword(@RequestHeader(value = "Authorization", required = true) String authorization,
                                                         @RequestBody @Valid ChangePasswordRequest request) {
        return Result.success("密码修改成功", userService.changePassword(request, authorization));
    }

    @PostMapping("/logout")
    @LogOperation("用户登出")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = true) String authorization) {
        userService.logout(authorization);
        return Result.success("登出成功", null);
    }

    /**
     * 邮箱脱敏
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" +domain;
        }
        return name.substring(0, 2) + "***@" +domain;
    }
}
