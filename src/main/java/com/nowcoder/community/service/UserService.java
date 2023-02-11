package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.pojo.LoginTicket;
import com.nowcoder.community.pojo.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private RedisTemplate redisTemplate;

    /*@Autowired
    private LoginTicketMapper loginTicketMapper;*/

    @Value("${community.path.domain}")
    private String domain; // 服务器网址

    @Value("${server.servlet.context-path}")
    private String contextPath; // 应用名

    public User findUserById(int id) {
        // return userMapper.selectById(id);
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    /**
     * 注册业务
     * @param user 输入的用户信息
     * @return 状态
     */
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (user == null) {
            throw  new IllegalArgumentException("参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "账号已存在！");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "邮箱已被注册！");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5)); // 设置随机加密码
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt())); // 设置加密后密码
        user.setType(0); // 设置类型为普通用户
        user.setStatus(0); // 设置状态为未激活
        user.setActivationCode(CommunityUtil.generateUUID()); // 随机生成状态码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000))); // 随机成成数字会替换 %d
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 给用户发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // url : http:localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map;
    }

    /**
     * 处理用户激活
     * @param userId 用户的id
     * @param code 激活码
     * @return 返回激活的状态 成功0 重复1 失败2
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 用户登录
     * 验证用户登陆传入的信息，如果正确生成登陆凭证返回，失败返回错误信息
     * @param username 用户名
     * @param password 密码
     * @param expireSeconds 登陆凭证存储的时间
     * @return 返回登陆凭证的 ticket 码
     */
    public Map<String, Object> login(String username, String password, long expireSeconds) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "账号不能为空！");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }

        // 验证密码
        System.out.println(password);
        password = CommunityUtil.md5(password + user.getSalt());
        System.out.println(password);
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码错误！");
            return map;
        }

        // 以上都没问题，说明登陆成功，生成登陆凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expireSeconds * 1000));
        /*loginTicketMapper.insertLoginTicket(loginTicket); // 存在数据库*/

        // 存在redis上
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;

    }

    /**
     * 退出登录
     * 修改数据库中的状态为 1 redis 中改
     * @param ticket 凭证码
     */
    public void logout(String ticket) {
        // loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    /**
     * 发送验证码
     * 验证输入的邮箱是否存在, 发送验证码
     * @param email 输入的邮箱
     * @return 错误信息 or 验证码
     */
    public Map<String, Object> verifyEmail(String email) {
        Map<String, Object> map = new HashMap<>();
        // 空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }
        User user = userMapper.selectByEmail(email);
        if (user == null) { // 不存在该邮箱注册的用户
            map.put("emailMsg", "查询不到该邮箱的注册信息！");
        } else {
            // 生成验证码
            String code = CommunityUtil.generateUUID().substring(0, 5);
            map.put("code", code);

            // 发送验证码
            Context context = new Context();
            context.setVariable("email", email);
            context.setVariable("code", code);
            String content = templateEngine.process("/mail/forget", context);
            mailClient.sendMail(user.getEmail(), "找回密码验证码", content);
        }
        return map;
    }

    /**
     * 重置密码
     * @param email 传入的邮箱
     * @param password 新密码
     * @return 错误信息
     */
    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        } else if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "查询不到该邮箱的注册信息！");
        } else {
            password = CommunityUtil.md5(password + user.getSalt());
            userMapper.updatePassword(user.getId(), password);
            clearCache(user.getId());
        }
        return map;
    }

    public LoginTicket findLoginTicket(String ticket) {
        // return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * 更改用户图片
     */
    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    /**
     * 修改密码
     */
    public Map<String, Object> updatePwd(int userId, String oldPwd, String newPwd) {
        Map<String, Object> map = new HashMap<>();

        User user = userMapper.selectById(userId);
        String pwd = CommunityUtil.md5(oldPwd + user.getSalt());
        if (!pwd.equals(user.getPassword())) {
            map.put("oldPwdMsg", "原密码错误！");
            return map;
        }

        // 修改密码
        newPwd = CommunityUtil.md5(newPwd + user.getSalt());
        userMapper.updatePassword(userId, newPwd);
        clearCache(userId);

        return map;
    }

    public User findUserByName(String userName) {
        return userMapper.selectByName(userName);
    }

    // 1. 优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2. 取不到时，初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS); // 有效时间 3600s 1h
        return user;
    }

    // 3. 数据变更时，清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthority(int userId) {
        User user = findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
