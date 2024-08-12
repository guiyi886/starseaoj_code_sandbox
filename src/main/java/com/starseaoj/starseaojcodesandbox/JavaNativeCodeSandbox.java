package com.starseaoj.starseaojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;

import java.io.File;
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
        String language = executeCodeRequest.getLanguage();

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

        // 隔离存放用户代码
        String userCodeParentPath = tmpCodePath + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
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
