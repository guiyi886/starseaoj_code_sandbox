package com.starseaoj.starseaojcodesandbox.codesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import com.starseaoj.starseaojcodesandbox.model.JudgeInfo;
import com.starseaoj.starseaojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author guiyi
 * @Date 2024/8/18 下午6:49:24
 * @ClassName com.starseaoj.starseaojcodesandbox.codesandbox.JavaCodeSandboxTemplate
 * @function --> 代码沙箱模板方法抽象类（骨架类）
 */
@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox {
    private static final String TMP_CODE_DIR = "tmpCode";

    private static final String JAVA_CLASS_NAME = "Main.java";

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 1.隔离存放用户代码
        File userCodeFile = saveCodeToFile(code);

        // 2.编译命令
        ExecuteMessage complieExecuteMessage = compileFile(userCodeFile);
        System.out.println(complieExecuteMessage);

        // 3.运行程序
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);

        // 4.整理输出结果
        ExecuteCodeResponse executeCodeResponse = getOutputResponse(executeMessageList);

        // 5.文件清理
        boolean isDel = deleteFile(userCodeFile);
        if (!isDel) {
            log.error("删除文件路径{}失败", userCodeFile.getAbsolutePath());
        }

        return executeCodeResponse;
    }

    /**
     * 1.隔离存放用户代码
     *
     * @param code
     * @return
     */
    public File saveCodeToFile(String code) {
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

        // 存放用户代码
        String userCodeParentPath = tmpCodePath + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);

        return userCodeFile;
    }

    /**
     * 2.编译命令
     *
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile) {
        String compileCmd = String.format("javac -encoding utf-8 %s", userCodeFile.getAbsolutePath());
        try {
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileCmd, "编译");
            if (executeMessage.getExistValue() != 0) {
                throw new RuntimeException("编译错误");
            }
            return executeMessage;
        } catch (IOException | InterruptedException e) {
            // return getErrorResponse(e);
            throw new RuntimeException("编译错误" + e);
        }
    }

    /**
     * 3.运行程序
     *
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s",
                    userCodeParentPath, inputArgs);
            try {
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runCmd, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                // return getErrorResponse(e);
                throw new RuntimeException("执行代码异常" + e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4.整理输出结果
     *
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();

        // 取所有测试用例的最大值
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)) {
                executeCodeResponse.setMessage(errorMessage);
                // 表示用户提交代码存在错误
                executeCodeResponse.setStatus(3);
                break;
            }
            if (maxTime < executeMessage.getTime()) {
                maxTime = executeMessage.getTime();
            }
            outputList.add(executeMessage.getMessage());
        }
        if (outputList.size() == executeMessageList.size()) {
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);

        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        // 要借助第三库实现，非常麻烦
        // judgeInfo.setMemory();
        executeCodeResponse.setJudgeInfo(judgeInfo);

        return executeCodeResponse;
    }

    /**
     * 5.文件清理
     *
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile().exists()) {
            boolean isDel = FileUtil.del(userCodeFile.getParentFile());
            System.out.println("删除" + (isDel ? "成功" : "失败"));
            return isDel;
        }
        return true;
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
}
