package com.anmory.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 缓存服务
 */
public interface RedisService {
    /**
     * 存储验证码
     * @param code 验证码
     * @param userId 用户ID
     * @param userName 用户名
     */
    void storeVerificationCode(String code, Long userId, String userName) throws JsonProcessingException;

    Long getUserIdByCode(String code);

    boolean isValidCode(String code);

    void remove(String code);
}
