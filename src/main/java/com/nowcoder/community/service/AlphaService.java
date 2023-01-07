package com.nowcoder.community.service;

import com.nowcoder.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @Author: HRD
 * @Date: 2023/1/4 16:26
 * @Description:
 */
@Service
//@Scope("prototype") // 多例
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    public AlphaService() {
        System.out.println("实例化 AlphaService");
    }

    @PostConstruct // 标注为初始化方法，会在构造器调用之后调用
    public void init() {
        System.out.println("初始化 AlphaService");
    }

    @PreDestroy // 标注为销毁方法，会在构造器销毁之前调用
    public void destroy() {
        System.out.println("销毁 AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }
}
