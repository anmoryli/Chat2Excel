package com.anmory.common.service;

/**
 * 发送邮件服务
 */
public interface EmailService {
    /**
     * 发送验证码
     * @param to 接收者邮箱
     * @param code 验证码
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String to, String code);
}
