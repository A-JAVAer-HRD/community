package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @Author: HRD
 * @Date: 2023/1/3 16:23
 * @Description:
 */
@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "Hello Spring Boot!";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getDate() {
        return alphaService.find();
    }

    @RequestMapping("/http")
    public void  http(HttpServletRequest request, HttpServletResponse response) {
        // 获取请求的数据
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());
        Enumeration<String> enumeration = request.getHeaderNames(); // 类似于 Iterator
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement(); // 请求行的名字
            String value = request.getHeader(name); // 对应的 value
            System.out.println(name + ": " + value);
        }
        System.out.println(request.getParameter("code"));

        // 返回响应数据
        response.setContentType("text/html;charset=utf-8"); // 设置返回类型
        try(
                PrintWriter writer = response.getWriter();
        ) {
            writer.write("<h1>牛客网</h1>"); // 输出流输出
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // GET请求
    // /student?current=1&limit=20
    @RequestMapping(path = "/student", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10")int limit) {
        System.out.println(current);
        System.out.println(limit);
        return "some students";
    }

    // /student/123 另一种请求方式
    @GetMapping("/student/{id}")
    @ResponseBody
    public String getStudent(@PathVariable("id") int id) {
        System.out.println(id);
        return "a student";
    }

    // POST 请求
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age) {
        System.out.println(name);
        System.out.println(age);
        return "success";
    }

    // 响应HTML数据
    @GetMapping("/teacher")
    public ModelAndView getTeacher() {
        ModelAndView mav = new ModelAndView();
        mav.addObject("name", "张三");
        mav.addObject("age", "30");
        mav.setViewName("/demo/view");
        return mav;
    }

    // 另一种写法
    @GetMapping("/school")
    public String getSchool(Model model) {
        model.addAttribute("name", "北大");
        model.addAttribute("age", "30");
        return "/demo/view";
    }

    // 响应JSON数据(一般是在异步请求当中)
    // java 对象 -> json -> js 对象
    @GetMapping("/emp")
    @ResponseBody
    public Map<String, Object> getEmp() {
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "张三");
        emp.put("年龄", 23);
        emp.put("salary", 8000.00);
        return emp;
    }

    // 集合类型的 json
    @GetMapping("/emps")
    @ResponseBody
    public List<Map<String, Object>> getEmps() {
        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "张三");
        emp.put("年龄", 23);
        emp.put("salary", 8000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name", "李四");
        emp.put("年龄", 24);
        emp.put("salary", 10000.00);
        list.add(emp);

        emp = new HashMap<>();
        emp.put("name", "王五");
        emp.put("年龄", 33);
        emp.put("salary", 18000.00);
        list.add(emp);

        return list;
    }

    /**
     * 测试设置cookie
     */
    @GetMapping("/cookie/set")
    @ResponseBody
    public String setCookie(HttpServletResponse response) {
        // 创建 cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        // 设置 cookie 生效的范围
        cookie.setPath("/community/alpha");
        // 设置 cookie 生存时间 20min
        cookie.setMaxAge(60 * 20);
        // 发送 cookie
        response.addCookie(cookie);

        return "set cookie";
    }

    /**
     * 测试 cookie
     */
    @GetMapping("/cookie/get")
    @ResponseBody
    public String setCookie(@CookieValue("code") String code) {
        System.out.println(code);
        return "get cookie";
    }

    /**
     * 测试创建session
     */
    @GetMapping("/session/set")
    @ResponseBody
    public String setSession(HttpSession session) {
        session.setAttribute("id", 1);
        session.setAttribute("name", "test");
        return "set session";
    }

    /**
     * 测试使用session
     */
    @GetMapping("/session/get")
    @ResponseBody
    public String getSession(HttpSession session) {
        System.out.println(session.getAttribute("id"));
        System.out.println(session.getAttribute("name"));
        return "get session";
    }

    // ajax实例
    @PostMapping("/ajax")
    @ResponseBody
    public String testAjax(String name, int age) {
        System.out.println(name);
        System.out.println(age);
        return CommunityUtil.getJSONString(0,"操作成功！");
    }

}
