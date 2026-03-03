package com.anmory.common.aspect;

import com.anmory.common.annotation.LogOperation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 操作日志切面  围绕LogOperation注解进行切面处理
 */
@Aspect
@Component
@Slf4j
public class LogOperationAspect {
    /*
    * 日志序列化打印
    * */
    private final ObjectMapper objectMapper;// final 是不可变的

    public LogOperationAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        //遇到空的 bean（没有任何可序列化属性的对象），别抛异常，直接输出一个空对象 {} 就行了。
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * 环绕通知，环绕LogOperation注解的方法
     */
    @Around("@annotation(com.anmory.common.annotation.LogOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Long startTime = System.currentTimeMillis();
        // 1. 访问的是哪个方法或者接口
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        LogOperation logOperation = method.getAnnotation(LogOperation.class);

        String methodName = method.getName();
        String operation = logOperation.value().isEmpty() ? methodName : logOperation.value();

        // 2. 使用servlet获取请求信息
        ServletRequestAttributes servletRequestAttribute = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttribute == null ? null : servletRequestAttribute.getRequest();

        // 3. 记录请求参数
        if (logOperation.logRequest()) {
            logRequest(operation, joinPoint, request);
        }

        // 4. 记录响应参数
        Object result = joinPoint.proceed(); // 需要先执行方法，再记录响应参数
        if (logOperation.logResponse()) {
            logResponse(operation, result, startTime, logOperation.logExecutionTime());
        }

        // 5. 响应的结果需要返回去
        return result;
    }

    /**
     * 记录请求参数
     * @param operation
     * @param joinPoint
     * @param request
     */
    public void logRequest(String operation, ProceedingJoinPoint joinPoint, HttpServletRequest request) throws JsonProcessingException {
        StringBuilder logBuilder = new StringBuilder();

        // 1. 记录请求方法名称
        logBuilder.append("[").append(operation).append("] 请求参数：");
        // 2. 记录请求路由和访问方法
        if(request != null) {
            logBuilder.append("method= ").append(request.getMethod()).append("url= ").append(request.getRequestURI());
            // 3. 加上请求参数
            String queryString = request.getQueryString();
            if(queryString != null && !queryString.isEmpty()) {
                logBuilder.append("query= ").append(markSensitiveData(queryString));
            }

            // 4. 获取请求头信息
            String authorization = request.getHeader("Authorization");

            if(authorization != null && !authorization.isEmpty()) {
                logBuilder.append("Authorization= ").append(maskToken(authorization));
            }
        }
        // 5. 记录方法参数
        Object args[] = joinPoint.getArgs();
        if(args != null && args.length > 0) {
            logBuilder.append(",args=[");
            // 遍历参数数组
            for(int i = 0; i < args.length; i++) {
                if(i > 0) {
                    logBuilder.append(", ");
                }
                Object arg = args[i];
                if(arg == null) {
                    logBuilder.append("null");
                } else if(isFile(arg)) { // 如果是文件
                    // 处理文件
                    logBuilder.append("name=").append(getFileName(arg));
                } else if(isHttpServletResponse(arg)) {// 判断是否是servlet对象
                    logBuilder.append("下载内容，无法打印");
                } else { // 序列化其他参数
                    String argStr = markSensitiveData(objectMapper.writeValueAsString(arg));
                    logBuilder.append(argStr);
                }
                logBuilder.append("]");
            }
            log.info(logBuilder.toString());
        }


    }

    public void logResponse(String operation, Object result, Long startTime, boolean logTime) throws JsonProcessingException {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("[").append(operation).append("] 响应结果：");

        if(result == null) {
            logBuilder.append("null");
        } else {
            // 序列化响应结果
            String resultStr = markSensitiveData(objectMapper.writeValueAsString(result));
            logBuilder.append(resultStr);
        }

        // 记录响应时间
        if(logTime) {
            logBuilder.append(", 访问耗时：").append(System.currentTimeMillis() - startTime).append("ms");
        }
        log.info(logBuilder.toString());
    }

    /**
     * 关键字段脱敏操作
     * @param data 原始字符串
     * @return 脱敏后的字符串
     */
    private String markSensitiveData(String data) {
        // 脱敏密码字段
        data = data.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
        data = data.replaceAll("\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"***\"");

        // 脱敏token字段
        data = data.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
        data = data.replaceAll("\"authorization\"\\s*:\\s*\"[^\"]*\"", "\"authorization\":\"***\"");

        // 脱敏邮箱
        data = data.replaceAll("\"email\"\\s*:\\s*\"([^\"]*@[^\"]*)\"", "\"email\":\"***\"");

        return data;
    }

    /**
     * 令牌脱敏
     * @param token 原始令牌
     * @return 脱敏后的Token
     */
    private String maskToken(String token) {
        return "****";
    }

    /**
     * 根据反射判断对象是否是文件
     * @param object 文件对象
     * @return 是或者否
     */
    private boolean isFile(Object object) {
        return object != null && object.getClass().getName().contains("MultipartFile");
    }

    /**
     * 根据反射靠对象来复原文件名
     * @param object 文件对象
     * @return 文件名
     */
    private String getFileName(Object object) {
        try {
            Method method =  object.getClass().getMethod("getOriginalFilename");
            return (String) method.invoke(object);
        } catch (Exception e) {
            return "文件未知!";
        }
    }

    /**
     * 根据反射来判断是否需要打印对象参数
     * @param object 入参
     * @return 是或者否
     */
    private boolean isHttpServletResponse(Object object) {
        return object != null && object.getClass().getName().contains("HttpServletResponse");
    }
}
