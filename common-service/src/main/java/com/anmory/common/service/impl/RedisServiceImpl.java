package com.anmory.common.service.impl;

import com.anmory.common.service.RedisService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务实现类
 */

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 为什么要创建一个ObjectMapper对象呢？
    // 1. 创建一个ObjectMapper对象，用于把对象转为JSON字符串
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证码前缀
     */
    private static final String CODE_PREFIX = "code:";

    /**
     * 个人令牌前缀
     */
    private static final String TOKEN_PREFIX = "token:";

    /**
     * 老用户令牌
     */
    private static final String USER_SESSION_PREFIX = "user_session:";

    /**
     * 存储验证码
     * @param code 验证码
     * @param userId 用户ID
     * @param userName 用户名
     * @throws JsonProcessingException
     */
    @Override
    public void storeVerificationCode(String code, Long userId, String userName) throws JsonProcessingException {
        // 1. 先生成key
        String key = CODE_PREFIX + code;
        CodeInfo codeInfo = new CodeInfo(userId, userName);

        // 2. 对象序列化之后再把值存到redis中
        String value = objectMapper.writeValueAsString(codeInfo);
        stringRedisTemplate.opsForValue().set(key, value, 300, TimeUnit.SECONDS);
        log.info("[redis验证码存储成功], 验证码{} 已经存储至{}", code, key);
    }

    /**
     * 根据验证码获取用户ID
     * @param code 验证码
     * @return 用户ID
     */
    @Override
    public Long getUserIdByCode(String code) {
        // 1. 先生成key
        String key = CODE_PREFIX + code;
        String value = stringRedisTemplate.opsForValue().get(key);

        if (value != null) {
            try {
                CodeInfo codeInfo = objectMapper.readValue(value, CodeInfo.class);
                return codeInfo.getUserId();
            } catch (JsonProcessingException e) {
                log.error("[redis验证码获取用户ID失败], 验证码{}", code);
            }
        }
        return null;
    }

    /**
     * 验证码是否有效
     * @param code 验证码
     * @return true:有效，false:无效
     */
    @Override
    public boolean isValidCode(String code) {
        // 1. 创建key
        String key = CODE_PREFIX + code;
        return stringRedisTemplate.hasKey(key);
    }

    @Override
    public void remove(String code) {
        stringRedisTemplate.delete(CODE_PREFIX + code);
        log.info("[redis验证码删除成功], 验证码{} 已经删除", code);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class CodeInfo{

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 用户名：邮箱或者用户名
         */
        private String userName;
    }
}
