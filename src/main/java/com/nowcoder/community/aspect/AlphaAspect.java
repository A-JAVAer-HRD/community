//package com.nowcoder.community.aspect;
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.*;
//import org.springframework.stereotype.Component;
//
//@Component
//@Aspect
//public class AlphaAspect {
//
//    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
//    // 第一个* 返回值
//    // com.nowcoder.community.service.*.* 包下的*组件的*方法
//    // (..)任意参数
//    public void pointCut() { // 标注切点
//
//    }
//
//    @Before("pointCut()")
//    public void before() { // 切点执行前
//        System.out.println("before");
//    }
//
//    @After("pointCut()")
//    public void after() { // 切点执行后
//        System.out.println("after");
//    }
//
//    @AfterReturning("pointCut()")
//    public void afterReturning() { // 切点返回值后
//        System.out.println("afterReturning");
//    }
//
//    @AfterThrowing("pointCut()")
//    public void afterThrowing() { // 切点抛出异常后
//        System.out.println("afterThrowing");
//    }
//
//    @Around("pointCut()")
//    public Object around(ProceedingJoinPoint joinPoint) throws Throwable { // 切点前或后
//        System.out.println("around before");
//        Object obj = joinPoint.proceed();// 调目标组件的方法
//        System.out.println("around after");
//        return obj;
//    }
//
//}
