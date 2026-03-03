package com.anmory.user.controller;

import com.anmory.common.annotation.LogOperation;
import com.anmory.common.util.Result;
import com.anmory.user.dto.request.SendCodeRequest;
import com.anmory.user.dto.response.SendCodeResponse;
import com.anmory.user.service.VerificationCodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户服务控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private VerificationCodeService verificationCodeService;
    /**
     * 发送邮箱验证码接口
     */
    @RequestMapping("/verification-code")
    @LogOperation("发送邮箱验证码")
    public Result<SendCodeResponse> sendVerificationCode(@RequestBody @Validated SendCodeRequest request) throws JsonProcessingException {
        int expireSeconds = verificationCodeService.sendCode(request.getEmail());
        String sendTo = request.getEmail();
        SendCodeResponse response = SendCodeResponse.builder()
                .sendTo(maskEmail(sendTo))
                .expireTime(expireSeconds)
                .build();
        return Result.success("验证码发送成功", response);
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
