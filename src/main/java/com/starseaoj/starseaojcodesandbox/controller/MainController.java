package com.starseaoj.starseaojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guiyi
 * @Date 2024/8/13 上午12:17:59
 * @ClassName com.starseaoj.starseaojcodesandbox.controller.MainController
 * @function --> 测试用
 */
@RestController
public class MainController {
    @GetMapping("ok")
    public String ok() {
        return "ok";
    }
}
