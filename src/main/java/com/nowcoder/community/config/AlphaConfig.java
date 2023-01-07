package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * @Author: HRD
 * @Date: 2023/1/4 16:39
 * @Description:
 */
@Configuration // 配置类
public class AlphaConfig {

    @Bean // 默认：方法名就是bean的名字
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
}
