package com.anmory.user.service.impl;

import com.anmory.common.service.EmailService;
import com.anmory.common.service.RedisService;
import com.anmory.user.entity.UserEntity;
import com.anmory.user.mapper.UserMapper;
import com.anmory.user.service.VerificationCodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 验证码服务实现类
 */

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisService redisService;
    @Override
    public int sendCode(String email) throws JsonProcessingException {
        // 1. 生成六位验证码
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        // 2. 发送验证码，写到通用服务里面去，可能后续其他服务也会用到发送邮件的服务
        boolean ifSendSuccess = emailService.sendVerificationCode(email, code);
        // 3. 查询数据库的判断逻辑 (根据邮箱去查询users表，存在即为登录，不存在即为注册)
        UserEntity user = userMapper.findByLoginKey(email);
        if(user == null) {
            // 新注册用户
            log.info("[新用户注册], 邮箱{}", email);
            user = new UserEntity();
            user.setId(0L); // id为0代表的是新用户
            user.setUserName(email);
            log.info("[新用户信息], 信息：{}", user);
        }

        // 验证码信息写进redis中
        redisService.storeVerificationCode(code, user.getId(), user.getUserName());

        if (ifSendSuccess) {
            log.info("[验证码发送成功], 验证码{} 已经发送至{}", code, email);
            return 300;
        } else {
            log.error("[验证码发送失败], 验证码{} 没有发送至{}", code, email);
            return -1;
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return redisService.isValidCode(code);
    }

    @Override
    public Long getUserId(String code) {
        return redisService.getUserIdByCode(code);
    }

    @Override
    public void remove(String code) {
        redisService.remove(code);
    }
}
