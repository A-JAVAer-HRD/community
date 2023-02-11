package com.nowcoder.community.dao;

import com.nowcoder.community.pojo.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: HRD
 * @Date: 2023/1/5 16:55
 * @Description:
 */
@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPost(int userId, int offset, int limit);

    // @Param注解用于给参数取别名
    // 如果只有一个参数，并且在<if>里使用，则必须加别名
    int selectDiscussPostRows(@Param("userId") int userId);

    int insertDiscussPost(DiscussPost discussPost);

    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(int id, int commentCount);

    List<DiscussPost> selectAllDiscussPosts();

    int updateType(int id, int type);

    int updateStatus(int id, int status);

}
