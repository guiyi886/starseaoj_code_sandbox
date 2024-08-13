package com.starseaoj.starseaojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import com.starseaoj.starseaojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author guiyi
 * @Date 2024/8/13 上午1:27:02
 * @ClassName com.starseaoj.starseaojcodesandbox.JavaNativeCodeSandbox
 * @function --> java原生代码沙箱
 */
public class JavaNativeCodeSandbox implements CodeSandbox {
    private static final String TMP_CODE_DIR = "tmpCode";

    private static final String JAVA_CLASS_NAME = "Main.java";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 根据资源路径推导出模块根目录
        // String userDir = System.getProperty("user.dir");  // 多模块时用该语句会获取成第一个模块的根目录
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("").getFile());
        String projectRoot = file.getParentFile().getParentFile().getPath();

        // 用File.separator，因为windows和linux的分隔符不一样，一个\\，一个/
        String tmpCodePath = projectRoot + File.separator + TMP_CODE_DIR;

        // 创建临时目录
        if (!FileUtil.exist(tmpCodePath)) {
            FileUtil.mkdir(tmpCodePath);
        }

        // 1.隔离存放用户代码
        String userCodeParentPath = tmpCodePath + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        // 2.编译命令
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileCmd, "编译");
            System.out.println(executeMessage);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 3.运行程序
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            ExecuteMessage executeMessage = null;
            try {
                executeMessage = ProcessUtils.runProcessAndGetMessage(runCmd, "运行");
                System.out.println(executeMessage);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    public static void main(String[] args) {
        ExecuteCodeRequest executeCodeRequest = new ExecuteCodeRequest();
        executeCodeRequest.setInputList(Arrays.asList("1 2", "3 4"));
        executeCodeRequest.setLanguage("java");

        // 获取Main.java文件
        String code = ResourceUtil.readStr("D:\\JavaProjects\\starseaoj_code_sandbox\\src\\test\\java\\com\\starseaoj\\starseaojcodesandbox\\Main.java",
                StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);

        // 调用代码沙箱
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
