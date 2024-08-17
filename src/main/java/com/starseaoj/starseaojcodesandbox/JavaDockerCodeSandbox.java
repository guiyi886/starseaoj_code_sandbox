package com.starseaoj.starseaojcodesandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import com.starseaoj.starseaojcodesandbox.model.JudgeInfo;
import com.starseaoj.starseaojcodesandbox.utils.ProcessUtils;
import org.springframework.util.StopWatch;

import java.io.Closeable;
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
    // 临时代码文件夹名
    private static final String TMP_CODE_DIR = "tmpCode";

    private static final String JAVA_CLASS_NAME = "Main.java";

    // 镜像名
    private static final String IMAGE_NAME = "openjdk:8-alpine";

    // 自定义容器名
    private static final String CONTAINER_NAME = "java8_container";

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
        // 创建 Docker 客户端
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();

        // 判断镜像是否存在
        if (!checkImageExists(dockerClient, IMAGE_NAME)) {
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(IMAGE_NAME);
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("下载镜像：" + item.getStatus());
                    super.onNext(item);
                }
            };
            try {
                pullImageCmd
                        .exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("下载镜像openjdk:8-alpine完成");
        }

        // 判断容器是否存在
        if (!checkContainerExists(dockerClient, CONTAINER_NAME)) {
            // 创建容器
            CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_NAME);
            HostConfig hostConfig = new HostConfig();
            hostConfig.withMemory(100 * 1000 * 1000L);
            hostConfig.withCpuCount(1L);
            hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));  // 文件路径映射

            CreateContainerResponse createContainerResponse = containerCmd
                    .withName(CONTAINER_NAME)    // 设置容器名称
                    .withHostConfig(hostConfig)
                    // .withNetworkDisabled(true)
                    // .withReadonlyRootfs(true)
                    .withAttachStdin(true)  // 与本地终端连接
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withTty(true)  // 创建交互终端
                    .exec();
            System.out.println("创建容器成功" + createContainerResponse);
        }
        // 启动容器
        dockerClient.startContainerCmd(CONTAINER_NAME).exec();

        // 4.在容器中执行代码，得到输出结果
        // docker exec java8_container java -cp /app Main 1 3
        // 执行命令并获取结果
        List<ExecuteMessage> executeMessageList = new ArrayList<>();
        for (String inputArgs : inputList) {
            String[] inputArgsArray = inputArgs.split(" ");
            String[] cmdArray = ArrayUtil.append(new String[]{"java", "-cp", "/app", "Main"}, inputArgsArray);
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(CONTAINER_NAME)
                    .withCmd(cmdArray)
                    .withAttachStderr(true)
                    .withAttachStdin(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("创建执行命令：" + execCreateCmdResponse);

            final String[] message = {null};
            final String[] errorMessage = {null};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload());
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload());
                        System.out.println("输出结果：" + message[0]);
                    }
                    super.onNext(frame);
                }
            };

            final long[] maxMemory = {0L};
            // 获取占用的内存
            StatsCmd statsCmd = dockerClient.statsCmd(CONTAINER_NAME);
            ResultCallback<Statistics> statisticsResultCallback = statsCmd.exec(new ResultCallback<Statistics>() {
                @Override
                public void onNext(Statistics statistics) {
                    System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() throws IOException {
                }

                @Override
                public void onStart(Closeable closeable) {
                }

                @Override
                public void onError(Throwable throwable) {
                }

                @Override
                public void onComplete() {
                }
            });
            statsCmd.exec(statisticsResultCallback);

            String execId = execCreateCmdResponse.getId();  // 获取容器id
            StopWatch stopWatch = new StopWatch();  // 计时
            long time = 0L;
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        .awaitCompletion();
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();

                statsCmd.close();   // 执行完后关闭统计命令
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }

        // 接下来的和java原生代码沙箱一致
        // 5.整理输出结果
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
        executeCodeResponse.setJudgeInfo(new JudgeInfo());

        // 6.文件清理
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
     * 判断某个镜像是否存在
     *
     * @param dockerClient
     * @param imageName
     * @return
     */
    private static boolean checkImageExists(DockerClient dockerClient, String imageName) {
        // 获取本地所有镜像的列表
        List<Image> images = dockerClient.listImagesCmd().exec();

        // 遍历镜像列表，检查是否包含指定镜像
        for (Image image : images) {
            for (String tag : image.getRepoTags()) {
                if (tag.equals(imageName)) {
                    return true;  // 镜像存在
                }
            }
        }
        return false;  // 镜像不存在
    }

    /**
     * 判断容器是否存在
     *
     * @param dockerClient
     * @param containerName
     * @return
     */
    private static boolean checkContainerExists(DockerClient dockerClient, String containerName) {
        // 获取所有运行中和停止的容器列表
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)  // 显示所有容器，包括停止的
                .withNameFilter(Arrays.asList(containerName))
                .exec();

        // 判断列表是否为空
        return !containers.isEmpty();
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
        // String code = ResourceUtil.readStr("testCode/unsafe/ReadFileError.java", StandardCharsets.UTF_8);
        String code = ResourceUtil.readStr("testCode/Main.java", StandardCharsets.UTF_8);
        executeCodeRequest.setCode(code);

        // 调用代码沙箱
        JavaDockerCodeSandbox javaNativeCodeSandbox = new JavaDockerCodeSandbox();
        ExecuteCodeResponse executeCodeResponse = javaNativeCodeSandbox.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }

}
