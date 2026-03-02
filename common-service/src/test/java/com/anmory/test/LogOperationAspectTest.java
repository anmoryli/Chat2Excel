package com.anmory.test;

import com.anmory.common.annotation.LogOperation;
import com.anmory.common.aspect.LogOperationAspect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// 测试用DTO
class LoginDTO {
    private String username;
    private String password;

    public LoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
}

// 用于生成真实Method的测试类
class TestService {
    @LogOperation(value = "测试登录", logRequest = true, logResponse = true, logExecutionTime = true)
    public void login(LoginDTO dto) {}
}

public class LogOperationAspectTest {

    private LogOperationAspect aspect;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() throws JsonProcessingException {
        MockitoAnnotations.openMocks(this);
        // 关键：模拟ObjectMapper序列化返回非null字符串，避免切面拿到null
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"username\":\"梦杰\",\"password\":\"fuck123\"}");
        aspect = new LogOperationAspect(objectMapper);
    }

    @Test
    public void testLogRequestWithSensitiveData() throws Throwable {
        // 1. 获取真实的Method对象
        Method realMethod = TestService.class.getMethod("login", LoginDTO.class);

        // 2. 模拟MethodSignature
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(realMethod);

        // 3. 模拟切入点参数和执行
        LoginDTO loginDTO = new LoginDTO("梦杰", "fuck123");
        when(joinPoint.getArgs()).thenReturn(new Object[]{loginDTO});
        when(joinPoint.proceed()).thenReturn(null);

        // 4. 模拟HTTP请求（确保所有getXXX方法不返回null）
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/test/login");
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        // 补充：模拟其他可能被切面调用的request方法，避免返回null
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        // 5. 设置请求上下文
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        // 6. 执行切面
        aspect.logOperation(joinPoint);

        // 7. 验证ObjectMapper调用
        verify(objectMapper, atLeastOnce()).writeValueAsString(any());

        // 8. 清理上下文
        RequestContextHolder.resetRequestAttributes();
    }
}