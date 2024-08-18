package com.starseaoj.starseaojcodesandbox.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author guiyi
 * @Date 2024/8/18 下午8:44:25
 * @ClassName com.starseaoj.starseaojcodesandbox.codesandbox.JavaNativeCodeSandboxNew
 * @function --> java代码沙箱模板实现
 */
@Component
public class JavaNativeCodeSandboxNew extends JavaCodeSandboxTemplate {
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        return super.executeCode(executeCodeRequest);
    }

    /**
     * 测试
     *
     * @param args
     */
    public static void main(String[] args) {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        executeCodeRequest.setLanguage("java");

        // 获取java文件
        String code = ResourceUtil.readStr("testCode/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);

        // 调用代码沙箱
        JavaNativeCodeSandboxNew javaNativeCodeSandboxNew = new JavaNativeCodeSandboxNew();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandboxNew.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
