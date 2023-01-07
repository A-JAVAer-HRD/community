package com.nowcoder.community.dao;

import com.nowcoder.community.pojo.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * @Author: HRD
 * @Date: 2023/1/5 15:10
 * @Description:
 */
@Mapper // 标识为一个 bean 和 @Repository 一样，之不过是 mybatis 的
public interface UserMapper {

    User selectById(int id) ;

    User selectByName (String username);

    User selectByEmail (String email);

    int insertUser(User user);

    int updateStatus (int id, int status);

    int updateHeader (int id, String headerUrl);

    int updatePassword(int id,String password);

}
