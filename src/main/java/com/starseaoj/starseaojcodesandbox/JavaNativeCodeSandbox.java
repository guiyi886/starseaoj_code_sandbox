package com.starseaoj.starseaojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
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
 * @author guiyi
 * @Date 2024/8/13 上午1:27:02
 * @ClassName com.starseaoj.starseaojcodesandbox.JavaNativeCodeSandbox
 * @function --> java原生代码沙箱
 */
public class JavaNativeCodeSandbox implements CodeSandbox {
    private static final String TMP_CODE_DIR = "tmpCode";

    private static final String JAVA_CLASS_NAME = "Main.java";

    private static final String SECURITY_MANAGER_PATH = "D:\\JavaProjects\\starseaoj_code_sandbox\\src\\main\\resources\\security";

    // 屏蔽关键字
    private static final List<String> BLOCK_LIST = Arrays.asList("Files", "exec", "bat", "rm");

    // 校检代码检查屏蔽词
    private static final WordTree WORD_TREE = new WordTree();

    static {
        // 加入字典树
        WORD_TREE.addWords(BLOCK_LIST);
    }


    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        // System.setSecurityManager(new DefaultSecurityManager());

        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();

        // 获取匹配到的屏蔽词
        FoundWord foundWord = WORD_TREE.matchWord(code);
        // if (foundWord != null) {
        //     // 输出屏蔽词
        //     System.out.println("包含屏蔽词" + foundWord.getFoundWord());
        //     return null;
        // }

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

        // 3.运行程序
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s;%s -Djava.security.manager=MySecurityManager Main %s",
                    userCodeParentPath, SECURITY_MANAGER_PATH, inputArgs);
            try {
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runCmd, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (IOException | InterruptedException e) {
                return getErrorResponse(e);
            }
        }

        // 4.整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        List<String> outputList = new ArrayList<>();

        // 取所有测试用例的最大值
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMassage();
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
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        // 5.文件清理
        if (userCodeFile.getParentFile().exists()) {
            boolean isDel = FileUtil.del(userCodeFile.getParentFile());
            System.out.println("删除" + (isDel ? "成功" : "失败"));
        }

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
        JavaNativeCodeSandbox javaNativeCodeSandbox = new JavaNativeCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

}
