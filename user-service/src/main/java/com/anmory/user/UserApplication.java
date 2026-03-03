package com.anmory.user;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 用户服务启动类
 */

@SpringBootApplication
@Slf4j
@MapperScan("com.anmory.user.mapper")
@ComponentScan(basePackages = {
        "com.anmory.common",
        "com.anmory.user"
})
public class UserApplication {
    public static void main(String[] args) {
        log.info("[用户服务启动] Port: 9001");
        org.springframework.boot.SpringApplication.run(UserApplication.class, args);
    }
}
