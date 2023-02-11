package com.nowcoder.community.controller;

import com.google.code.kaptcha.Producer;
import com.nowcoder.community.pojo.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: HRD
 * @Date: 2023/1/7 20:55
 * @Description:
 */
@Slf4j
@Controller
public class LoginController implements CommunityConstant {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 注册页面
     * @return 注册页面的 view
     */
    @GetMapping("/register")
    public String getRegister() {
        return "/site/register";
    }

    /**
     * 登录页面
     * @return 登录页面的 view
     */
    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }

    /**
     * 忘记密码
     * @return 忘记密码的 view
     */
    @GetMapping("/forget")
    public String getForgetPage() {
        return "/site/forget";
    }

    /**
     * 注册行为 post提交表单
     * @param model 模板数据
     * @param user 表单的数据
     * @return 注册页面的 view
     */
    @PostMapping("/register")
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活!");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    /**
     * 处理激活用户行为
     * url : http:localhost:8080/community/activation/101/code
     */
    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model, @PathVariable("userId")int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功，您的帐号可以正常使用了!");
            model.addAttribute("target", "/login"); // target 是路径
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作，该用户已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败，您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

    /**
     * 生成验证码图片
     */
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入 session
        // session.setAttribute("kaptcha", text);

        // 验证码的归属,生成随机字符串来作为redis生成key拼接的一部分，把字符串通过cookie发给用户
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60); // 有效时间 60s
        cookie.setPath(contextPath); // 有效路径
        response.addCookie(cookie);

        // 将验证码存入 redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS); // 有效期 60s

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            log.error("响应验证码失败" + e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public String login(String username, String password, String code, boolean rememberMe,
                        Model model, /*HttpSession session,*/ HttpServletResponse response,
                        @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner) {
        // 取出验证码,检查验证码
        // String kaptcha = (String) session.getAttribute("kaptcha");
        String kaptcha = null;
        if (StringUtils.isNoneBlank(kaptchaOwner)) {
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确！");
            return "/site/login"; // 返回登陆页面
        }
        // 检查账号，密码
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) {
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index"; // 重定向到首页
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login"; // 返回登陆页面
        }
    }

    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        SecurityContextHolder.clearContext();
        return "redirect:/login";
    }

    /**
     * 发送验证码
     */
    @GetMapping("/forget/code")
    @ResponseBody
    public String getForgetCode(String email, HttpSession session) {
        Map<String, Object> map = userService.verifyEmail(email);
        if (map.containsKey("emailMsg")) {
            return CommunityUtil.getJSONString(1, map.get("emailMsg").toString());
        }
        if (map.containsKey("code")) {
            // 保存验证码
            session.setAttribute("verifyCode", map.get("code"));
            return CommunityUtil.getJSONString(0);
        }
        return "";
    }

    /**
     * 修改密码
     */
    @PostMapping("/forget")
    public String resetPassword(String email, String verifycode, String password, HttpSession session, Model model) {
        Object attribute = session.getAttribute("verifyCode");
        String code = null;
        if (attribute != null) {
            code = attribute.toString();
        } else {
            model.addAttribute("codeMsg", "验证码过期！");
        }
        if (StringUtils.isBlank(code) || StringUtils.isBlank(verifycode) || !code.equalsIgnoreCase(verifycode)) {
            model.addAttribute("codeMsg", "验证码错误！");
            return "/site/forget";
        }

        Map<String, Object> map = userService.resetPassword(email, password);
        System.out.println(map);
        if (map.size() == 0) { // 修改成功，通过 result 页面渲染，跳转到 login 页面
            model.addAttribute("msg", "密码修改成功");
            model.addAttribute("target", "/index");
            System.out.println("修改成功！！！！");
            return "/site/operate-result";
        } else { // 提示错误信息
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            System.out.println("粗无！！！");
            return "/site/forget";
        }
    }
}
