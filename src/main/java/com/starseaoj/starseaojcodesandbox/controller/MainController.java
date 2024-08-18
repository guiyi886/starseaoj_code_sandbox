package com.starseaoj.starseaojcodesandbox.controller;

import com.starseaoj.starseaojcodesandbox.codesandbox.JavaNativeCodeSandboxNew;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author guiyi
 * @Date 2024/8/13 上午12:17:59
 * @ClassName com.starseaoj.starseaojcodesandbox.controller.MainController
 * @function --> 控制层
 */
@RestController
public class MainController {

    @Resource
    private JavaNativeCodeSandboxNew javaNativeCodeSandboxNew;

    @GetMapping("ok")
    public String ok() {
        return "ok";
    }

    /**
     * 调用代码沙箱执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    @PostMapping("/executeCode")
    public ExecuteCodeResponse executeCode(@RequestBody ExecuteCodeRequest executeCodeRequest) {
        if (executeCodeRequest == null) {
            throw new RuntimeException("请求参数为空");
        }
        return javaNativeCodeSandboxNew.executeCode(executeCodeRequest);
    }
}
