package com.anmory.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus 核心配置类
 */

@Configuration
public class MybatisPlusConfig {

    /**
     * 创建分页拦截器插件
     * @return MybatisPlusInterceptor
     */
    @Bean // 交给spring管理，不用自己new，是单例模式，无论自动注入多少都是同一个实例，线程安全
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单次最大查询数量
        paginationInnerInterceptor.setMaxLimit(500L);
        // 请求页码超出总页数，就返回空
        paginationInnerInterceptor.setOverflow(false);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
