package com.starseaoj.starseaojcodesandbox.codesandbox;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;
import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.io.Closeable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author guiyi
 * @Date 2024/8/18 下午8:46:13
 * @ClassName com.starseaoj.starseaojcodesandbox.codesandbox.JavaDockerCodeSandboxNew
 * @function --> docker代码沙箱模板实现
 */
@Component
public class JavaDockerCodeSandboxNew extends JavaCodeSandboxTemplate {
    private static final long TIME_OUT = 5000L;

    // 镜像名
    private static final String IMAGE_NAME = "openjdk:8-alpine";

    // 自定义容器名
    private static final String CONTAINER_NAME = "java8_container";

    /**
     * 创建容器并执行代码
     *
     * @param userCodeFile
     * @param inputList
     * @return
     */
    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
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
        // 注意容器不可复用，因为每次的挂载目录都不同，且docker 不支持直接修改已经创建的容器的挂载目录。
        // 因此只能删除后重新创建容器并挂载目录。
        if (checkContainerExists(dockerClient, CONTAINER_NAME)) {
            // 先停止并删除旧容器
            dockerClient.removeContainerCmd(CONTAINER_NAME).withForce(true).exec();
        }
        // 创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(IMAGE_NAME);
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(100 * 1000 * 1000L);
        hostConfig.withMemorySwap(0L);
        hostConfig.withCpuCount(1L);
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        hostConfig.setBinds(new Bind(userCodeParentPath, new Volume("/app")));  // 文件路径映射
        // 配置seccomp
        String profileConfig = ResourceUtil.readUtf8Str("seccomp/profile.json");
        hostConfig.withSecurityOpts(Arrays.asList("seccomp=" + profileConfig));

        CreateContainerResponse createContainerResponse = containerCmd
                .withName(CONTAINER_NAME)    // 设置容器名称
                .withHostConfig(hostConfig)
                .withNetworkDisabled(true)  // 禁用网络
                .withReadonlyRootfs(true)   // 禁止向root根目录写文件
                .withAttachStdin(true)  // 与本地终端连接
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)  // 创建交互终端
                .exec();
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
            final boolean[] timeout = {true}; // 超时标志
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback() {
                @Override
                public void onComplete() {
                    // 如果执行完成，设置为false表示未超时
                    timeout[0] = false;
                    super.onComplete();
                }

                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if (StreamType.STDERR.equals(streamType)) {
                        errorMessage[0] = new String(frame.getPayload()).substring(0, frame.getPayload().length - 1);
                        System.out.println("输出错误结果：" + errorMessage[0]);
                    } else {
                        message[0] = new String(frame.getPayload()).substring(0, frame.getPayload().length - 1);
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
                    // System.out.println("内存占用：" + statistics.getMemoryStats().getUsage());
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                }

                @Override
                public void close() {
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
                        .awaitCompletion(TIME_OUT, TimeUnit.MILLISECONDS);  // 设置超时时间
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();

                // statisticsResultCallback.close();   // 执行完后关闭统计命令
                statsCmd.close();
            } catch (InterruptedException e) {
                System.out.println("程序执行异常");
                throw new RuntimeException(e);
            }
            ExecuteMessage executeMessage = new ExecuteMessage();
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0] / 1024);
            executeMessageList.add(executeMessage);
        }
        dockerClient.stopContainerCmd(CONTAINER_NAME).exec();   // 停止容器
        return executeMessageList;
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
     * 判断容器是否在运行
     *
     * @param dockerClient
     * @param containerName
     * @return
     */
    private static boolean isContainerRunning(DockerClient dockerClient, String containerName) {
        InspectContainerResponse containerResponse = dockerClient.inspectContainerCmd(containerName).exec();
        InspectContainerResponse.ContainerState state = containerResponse.getState();
        return state.getRunning();
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
        JavaDockerCodeSandboxNew javaDockerCodeSandboxNew = new JavaDockerCodeSandboxNew();
        ExecuteCodeResponse executeCodeResponse = javaDockerCodeSandboxNew.executeCode(executeCodeRequest);
        System.out.println(executeCodeResponse);
    }
}
