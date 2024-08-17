package com.starseaoj.starseaojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import com.starseaoj.starseaojcodesandbox.model.JudgeInfo;
import com.starseaoj.starseaojcodesandbox.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * docker实现代码沙箱
 */
public class JavaDockerCodeSandbox implements CodeSandbox {
    private static final String TMP_CODE_DIR = "tmpCode";

    private static final String JAVA_CLASS_NAME = "Main.java";

    private static final String SECURITY_MANAGER_PATH = "D:\\JavaProjects\\starseaoj_code_sandbox\\src\\main\\resources\\security";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // System.setSecurityManager(new DefaultSecurityManager());

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
            return getErrorResponse(e);
        }

        // 3.创建容器，复制文件到其中
        

        // todo
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        return executeCodeResponse;
    }

    /**
     * 6.错误处理
     * 获取错误响应
     *
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Exception e) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        // 表示代码沙箱错误
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        return executeCodeResponse;
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
        String code = ResourceUtil.readStr("testCode/unsafe/ReadFileError.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);

        // 调用代码沙箱
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

}
