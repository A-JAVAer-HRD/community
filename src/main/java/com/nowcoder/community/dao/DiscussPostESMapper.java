package com.nowcoder.community.dao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.pojo.DiscussPost;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Repository
public class DiscussPostESMapper {

    @Autowired
    private RestHighLevelClient client;

    public DiscussPost getPostById(String id) {
        try {
            // 1.准备Request
            GetRequest request = new GetRequest("post", id);
            // 2.发送请求，得到响应
            GetResponse response = client.get(request, RequestOptions.DEFAULT);
            // 3.解析响应结果
            String json = response.getSourceAsString();
            return JSON.parseObject(json, DiscussPost.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deletePostById(String id) {
        try {
            // 1.准备Request
            DeleteRequest request = new DeleteRequest("post", id);
            // 2.发送请求
            client.delete(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addPost(DiscussPost post) {
        try {
            // 1.将HotelDoc转json
            String json = JSONObject.toJSONString(post);

            // 1.准备Request对象
            IndexRequest request = new IndexRequest("post").id(String.valueOf(post.getId()));
            // 2.准备Json文档
            request.source(json, XContentType.JSON);
            // 3.发送请求
            client.index(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int searchCount(String keyword) {
        try {
            // 1.准备Request
            SearchRequest request = new SearchRequest("post");
            // 2.准备DSL
            // 2.1.query
            request.source()
                    .query(QueryBuilders.multiMatchQuery(keyword, "content", "title"));
            // 3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.返回数量
            return Math.toIntExact(response.getHits().getTotalHits().value);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<DiscussPost> search(String keyword, int offset, int limit) {
        try {
            // 1.准备Request
            SearchRequest request = new SearchRequest("post");
            // 2.准备DSL
            // 2.1.query
            request.source()
                    .query(QueryBuilders.multiMatchQuery(keyword, "content", "title"));
            // 2.2.高亮
            request.source().highlighter(new HighlightBuilder().field("title").requireFieldMatch(false).field("content").requireFieldMatch(false));
            // 2.3.分页
            request.source().from(offset).size(limit);
            // 3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.解析响应
            return handleResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    // 处理响应结果
    private List<DiscussPost> handleResponse(SearchResponse response) {
        List<DiscussPost> posts = new LinkedList<>();
        // 4.解析响应
        SearchHits searchHits = response.getHits();
        // 4.2.文档数组
        SearchHit[] hits = searchHits.getHits();
        // 4.3.遍历
        for (SearchHit hit : hits) {
            // 获取文档source
            String json = hit.getSourceAsString();
            // 反序列化
            DiscussPost post = JSON.parseObject(json, DiscussPost.class);
            // 获取高亮结果
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                // 根据字段名获取高亮结果
                HighlightField titleField = highlightFields.get("title");
                HighlightField contentField = highlightFields.get("content");
                if (titleField != null) {
                    // 获取高亮值
                    String title = titleField.getFragments()[0].string();
                    // 覆盖非高亮结果
                    post.setTitle(title);
                }
                if (contentField != null) {
                    // 获取高亮值
                    String content = contentField.getFragments()[0].string();
                    // 覆盖非高亮结果
                    post.setContent(content);
                }
            }
            posts.add(post);
        }
        return posts;
    }
}
