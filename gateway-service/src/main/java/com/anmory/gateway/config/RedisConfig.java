package com.anmory.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 */
@Configuration
public class RedisConfig {


    /**
     * 构建 ReactiveRedisTemplate
     * @param connectionFactory 连接工厂
     * @return ReactiveRedisTemplate示例对象
     */
    @Bean
    public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        // 1. 创建字符串序列化器（key 和 value 都用字符串）
        StringRedisSerializer serializer = new StringRedisSerializer();

        // 2. 创建序列化上下文构建器
        RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
                RedisSerializationContext.newSerializationContext();

        // 3. 配置 key、value、hashKey、hashValue 的序列化方式
        RedisSerializationContext<String, String> context = builder
                .key(serializer)        // key 用字符串
                .value(serializer)      // value 用字符串
                .hashKey(serializer)    // hash 的 field 用字符串
                .hashValue(serializer)  // hash 的 value 用字符串
                .build();

        // 4. 用连接工厂 + 序列化上下文 创建 ReactiveRedisTemplate
        return new ReactiveRedisTemplate<>(connectionFactory, context);
    }
}
