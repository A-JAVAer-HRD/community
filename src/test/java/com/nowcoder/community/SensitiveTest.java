package com.nowcoder.community;

import com.nowcoder.community.pojo.DiscussPost;
import com.nowcoder.community.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.HtmlUtils;

import java.util.Date;

@SpringBootTest
public class SensitiveTest {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void sensitiveTest() {
        String text = "⭐赌一博了⭐,⭐嫖娼啊⭐，拉拉，吸毒开票，fabc";
        System.out.println(sensitiveFilter.filter(text));
        System.out.println("哈哈，");
    }

    @Test
    public void sensitiveHtmlTest() {
        DiscussPost discussPost = new DiscussPost();
        String title = "<script>alert('哈哈');</script>";
        String content = "<h1>可以开票，+QQ:112333</h1>";
        discussPost.setUserId(1);
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        System.out.println(discussPost);

        // 转意HTML标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));
        // 过滤敏感词
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));
        System.out.println(discussPost);
    }

}
