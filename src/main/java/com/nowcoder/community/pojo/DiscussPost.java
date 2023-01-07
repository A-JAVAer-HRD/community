package com.nowcoder.community.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @Author: HRD
 * @Date: 2023/1/5 16:53
 * @Description:
 */
@Data
public class DiscussPost {

    private int id;
    private int userId;
    private String title;
    private String content;
    private int type;
    private int status;
    private Date createTime;
    private int commentCount;
    private double score;

}
