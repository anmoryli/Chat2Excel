package com.anmory.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义操作日志注解
 * 使用方式：在需要记录操作日志的方法上加 @LogOperation
 * 比如：@LogOperation("用户登录") public void login() { ... }
 * <p>
 * 注解会在运行时通过AOP（切面）拦截方法，记录操作人、时间、IP、方法名、参数、结果等信息
 */
@Target(ElementType.METHOD)
// @Target 指定这个注解“能用在什么地方”
// ElementType.METHOD 表示：只能加在【方法】上面
// 如果写成 ElementType.TYPE 就是类/接口上面，ElementType.FIELD 就是字段上面
// 多个地方可以用大括号：@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
// @Retention 指定这个注解“保留到什么时候”
// RetentionPolicy.RUNTIME 表示：编译后保留到运行时（JVM能读到）
// 这是最常用的，因为我们通常用反射或AOP在运行时读取这个注解的信息
// 其他两种：
//   - SOURCE：只在源代码阶段存在，编译后就没了（比如 @Override）
//   - CLASS：编译后保留在class文件里，但运行时JVM不加载（很少用）
public @interface LogOperation { // 加@符号是说明这个是一个注解
    /**
     * 操作描述 知道是干什么的，就是接口名称
     */
    String value() default "";

    /**
     * 是否需要记录请求参数
     */
    boolean logRequest() default true;

    /**
     * 是否需要记录响应参数
     */
    boolean logResponse() default true;

    /**
     * 是否需要记录接口执行时间
     */
    boolean logExecutionTime() default true;
}
