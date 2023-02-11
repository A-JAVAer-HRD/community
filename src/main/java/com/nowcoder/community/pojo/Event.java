package com.nowcoder.community.pojo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Event {

    private String topic;

    /**
     * 触发事件的用户
     */
    private int userId;
    private int entityType;
    private int entityId;

    /**
     * 对应的实体的作者
     */
    private int entityUserId;
    private Map<String, Object> data;

    public Event() {
        this.data = new HashMap<>();
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
