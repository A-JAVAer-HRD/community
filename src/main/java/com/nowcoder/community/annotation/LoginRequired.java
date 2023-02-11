package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明注解
 */
@Target(ElementType.METHOD) // 生命在方法之上，描述方法
@Retention(RetentionPolicy.RUNTIME) // 只在运行时有效
public @interface LoginRequired {

}
