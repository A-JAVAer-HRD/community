package com.nowcoder.community.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @Author: HRD
 * @Date: 2023/1/5 15:02
 * @Description:
 */
@Data
public class User {
    private int id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createTime;
}
