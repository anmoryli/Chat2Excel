package com.anmory.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关路由配置类
 *
 * 这个类的主要作用是：
 * 使用 Spring Cloud Gateway 的 RouteLocatorBuilder 来定义所有路由规则
 * 所有外部请求都会先经过这里进行匹配和转发
 *
 * 当前是空的 routes().build()，说明还没有定义任何具体路由
 * 实际项目中，你需要在这里添加各种 routes() 调用来配置转发规则
 */
@Configuration
public class GateWayConfig {

    /**
     * 定义网关的路由规则 Bean
     *
     * Spring Cloud Gateway 启动时会自动加载这个 RouteLocator
     * RouteLocatorBuilder 是构建路由的 DSL 工具
     *
     * @param builder Spring 提供的路由构建器
     * @return RouteLocator 路由定位器（包含所有路由规则）
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        // 当前是空的配置，实际项目中应该在这里添加路由规则
        // 示例写法如下（已注释）：
        return builder.routes()
                // 示例1：所有 /user/** 的请求转发到 user-service 服务
                /*
                .route("user-service-route", r -> r
                    .path("/user/**")
                    .uri("lb://user-service")  // lb:// 表示负载均衡，从注册中心找服务
                )
                */

                // 示例2：带过滤器的路由（添加请求头、限流、鉴权等）
                /*
                .route("order-service-route", r -> r
                    .path("/order/**")
                    .filters(f -> f
                        .addRequestHeader("X-Request-Source", "gateway")  // 添加自定义请求头
                        .requestRateLimiter(rl -> rl.setRateLimiter(redisRateLimiter())) // 限流
                    )
                    .uri("lb://order-service")
                )
                */

                // 示例3：重写路径（把 /api/v1/** 去掉前缀转发）
                /*
                .route("api-route", r -> r
                    .path("/api/v1/**")
                    .filters(f -> f.stripPrefix(2))  // 去掉 /api/v1 两段
                    .uri("lb://backend-service")
                )
                */

                // 当前实际返回空的路由集合
                .build();
    }

    /**
     * 如果你需要自定义限流器，可以在这里定义一个 Bean
     * （示例，未启用）
     */
    /*
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20); // 每秒10个请求，突发20个
    }
    */
}
