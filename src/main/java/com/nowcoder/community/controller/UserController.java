package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.pojo.Comment;
import com.nowcoder.community.pojo.DiscussPost;
import com.nowcoder.community.pojo.User;
import com.nowcoder.community.service.*;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.nowcoder.community.util.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @LoginRequired
    @GetMapping("/setting")
    public String getSetting() {
        return "/site/setting";
    }

    /**
     * 上传文件
     * @param headerImage MultipartFile文件 springMVC专门用来上传文件的
     * @param model 返回的数据
     * @return 模板
     */
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择文件！");
            return "/site/setting";
        }

        // 设置新的文件名
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确！");
            return "/site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;

        // 确定文件的存储路径
        File file = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(file);
        } catch (IOException e) {
            log.error("上传文件失败" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！" ,e);
        }

        // 更新当前用户的头像路径 (web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:/index";
    }

    /**
     * 返回请求的图片
     * <p>
     * 通过HttpServletResponse的字节流输出
     */
    @GetMapping("/header/{fileName}")
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器实际的存储路径
        fileName = uploadPath + "/" + fileName;
        // 获取文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        // 响应图片
        response.setContentType("image/" + suffix);

        try (
                FileInputStream fis = new FileInputStream(fileName); // ()内的会自动调用close方法关闭
                OutputStream os = response.getOutputStream();
                ){
            byte[] buffer = new byte[1024];
            int b = 0; // 游标
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer);
            }
        } catch (IOException e) {
            log.error("读取头像失败！" + e.getMessage());
        }
    }

    @LoginRequired
    @PostMapping("/update")
    public String updatePassword(String oldPwd, String newPwd, String confirmPwd, Model model) {
        User user = hostHolder.getUser();

        if (StringUtils.isBlank(oldPwd)) {
            model.addAttribute("oldPwdMsg", "原密码不能为空！");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPwd)) {
            model.addAttribute("newPwdMsg", "新密码不能为空！");
            return "/site/setting";
        }
        if (StringUtils.isBlank(confirmPwd)) {
            model.addAttribute("confirmPwdMsg", "确认密码不能为空！");
            return "/site/setting";
        }
        if (!newPwd.equals(confirmPwd)) {
            model.addAttribute("confirmPwdMsg", "两次密码不一致！");
            return "/site/setting";
        }

        Map<String, Object> map = userService.updatePwd(user.getId(), oldPwd, newPwd);
        if (map.size() > 0) {
            model.addAttribute("oldPwdMsg", map.get("oldPwdMsg"));
            return "/site/setting";
        } else {
            return "redirect:/logout";
        }
    }

    // 个人主页
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw  new RuntimeException("该用户不存在！");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注的数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 是否已关注
        boolean hasFollowed = true;
        if (hostHolder.getUser() != null) {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);
        return "/site/profile";
    }

    // 我的帖子
    @GetMapping("/mypost/{userId}")
    public String getMyPost(@PathVariable("userId") int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user", user);
        // 配置分页参数
        page.setLimit(10);
        page.setPath("/user/mypost" + userId);
        page.setRows(discussPostService.findDiscussPostRows(userId));
        // 查询用户发布的帖子
        List<DiscussPost> discussList = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussPosts = new LinkedList<>();
        if (discussList != null) {
            for (DiscussPost discussPost : discussList) {
                Map<String, Object> map = new HashMap<>();
                map.put("discussPost", discussPost);
                // 查询帖子的点赞数,放入map中
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));

                discussPosts.add(map);
            }
        }

        model.addAttribute("discussPosts", discussPosts);
        return "/site/my-post";
    }

    // 我的回复
    @GetMapping("/myreply/{userId}")
    public String getMyReply(@PathVariable("userId")int userId, Page page, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user", user);
        // 设置分页参数
        page.setLimit(10);
        page.setPath("/user/myreply" + userId);
        page.setRows(commentService.findUserCount(userId));
        // 查找用户的回复
        List<Comment> commentList = commentService.findUserComments(userId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> comments = new LinkedList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                Map<String, Object> map = new HashMap<>();
                map.put("comment", comment);
                // 查询对应的帖子
                DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
                map.put("discussPost", post);
                comments.add(map);
            }
        }
        model.addAttribute("comments", comments);
        return "/site/my-reply";
    }
}
