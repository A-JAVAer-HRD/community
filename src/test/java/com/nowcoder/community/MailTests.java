package com.nowcoder.community;

import com.nowcoder.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * @Author: HRD
 * @Date: 2023/1/7 17:29
 * @Description:
 */
@SpringBootTest
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    public void testTextMail() {
        mailClient.sendMail("2045185019@qq.com", "TEST", "hello");
    }

    @Test
    public void testHtmlMail() {
        Context context = new Context(); // 要发送的数据内容
        context.setVariable("username", "sunday"); // 设置数据

        String content = templateEngine.process("/mail/demo.html", context); // 调用模板引擎，返回 html 字符串
        System.out.println(content);

        mailClient.sendMail("2045185019@qq.com", "HTML", content); // 发送邮件
    }

    @Test
    public void testForgetMail() {
        // 发送验证码
        Context context = new Context();
        context.setVariable("email", "email");
        context.setVariable("code", "code");
        String content = templateEngine.process("/mail/forget", context);
        System.out.println(content);
    }

    @Test
    public void testActivationMail() {
        // 给用户发送激活邮件
        Context context = new Context();
        context.setVariable("email", "user");
        // url : http:localhost:8080/community/activation/101/code
        context.setVariable("url", "url");
        String content = templateEngine.process("/mail/activation", context);
        System.out.println(content);
    }
}
