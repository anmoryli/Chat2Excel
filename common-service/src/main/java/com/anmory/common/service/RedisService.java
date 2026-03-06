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

    /**
     * 把生成好的token存储到redis
     * @param token 令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param expireSeconds 过期时间秒数
     */
    void storeToken(String token, Long userId, String username, long expireSeconds) throws JsonProcessingException;


    /**
     * 用于处理重复登录
     * @param userId 用户ID
     * @param token 令牌
     * @param expireSeconds 过期时间秒数
     */
    void storeUserActiveToken(Long userId, String token, long expireSeconds);

    /**
     * 获取用户的token
     * @param userId 用户ID
     * @return 用户token
     */
    String getUserToken(Long userId);

    /**
     * 判断用户是否登录
     * @param userId 用户ID
     * @return 是否登录
     */
    boolean isUserLogged(Long userId);

    /**
     * 根据令牌来获取用户ID
     * @param authorization 令牌
     * @return 用户ID
     */
    Long getUserIdByAuthorization(String authorization);

    /**
     * 删除用户令牌
     * @param authorization 用户令牌
     */
    void removeAuthorization(String authorization);
}
