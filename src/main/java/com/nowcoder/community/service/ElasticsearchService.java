package com.nowcoder.community.service;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.DiscussPostESMapper;
import com.nowcoder.community.pojo.DiscussPost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostESMapper discussPostESMapper;

    public void saveDiscussPost(DiscussPost post) {
        discussPostESMapper.addPost(post);
    }

    public void deleteDiscussPost(int id) {
        discussPostESMapper.deletePostById(String.valueOf(id));
    }

    public List<DiscussPost> search(String keyword, int offset, int limit) {
        return discussPostESMapper.search(keyword, offset, limit);
    }

    public int searchCount(String keyword) {
        return discussPostESMapper.searchCount(keyword);
    }

}
