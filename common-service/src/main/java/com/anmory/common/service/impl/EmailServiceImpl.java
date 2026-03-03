package com.anmory.common.service.impl;

import com.anmory.common.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现类
 */

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    /**
     * Spring 邮件发送器
     */
    @Autowired
    private JavaMailSender mailSender;

    /**
     * 发送方的邮箱
     */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 获取邮件内容
     * @param code 验证码
     * @return 邮件内容
     */
    private String getContent(String code) {
        return String.format("您好！"+
                "您的验证码是：%s\n\n"+
                "验证码的有效期是5分钟，请及时使用。",
                code
                );
    }

    @Override
    public boolean sendVerificationCode(String to, String code) {
        // 发送邮件的逻辑

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);

        String subject = "系统验证码";
        String content = getContent(code);

        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
        log.info("[发送邮件] 验证码 {}  已发送至 {}", code, to);
        return true;
    }
}
