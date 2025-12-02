package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendResetPasswordEmail(String email, String resetLink) {
        // 实际项目中应该实现邮件发送逻辑
        // 这里只是记录日志
        log.info("发送重置密码邮件到: {}, 重置链接: {}", email, resetLink);

        // 如果是开发环境，可以直接打印链接
        System.out.println("重置密码链接: " + resetLink);
    }
}