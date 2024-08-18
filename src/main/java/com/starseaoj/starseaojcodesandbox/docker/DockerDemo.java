package com.starseaoj.starseaojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.LogContainerResultCallback;

import java.util.List;

/**
 * @author guiyi
 * @Date 2024/8/17 上午1:40:35
 * @ClassName com.starseaoj.starseaojcodesandbox.docker.DockerDemo
 * @function --> 测试DockerClient
 */
public class DockerDemo {
    public static void main(String[] args) throws InterruptedException {
        DockerClient dockerClient = null;

        // 创建 Docker 客户端
        dockerClient = DockerClientBuilder.getInstance().build();

        // 列出所有容器，包括已停止的容器
        List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
        if (containers.isEmpty()) {
            System.out.println("没有容器正在运行。");
        } else {
            for (Container container : containers) {
                System.out.println("Container ID: " + container.getId() + ", Names: " + String.join(", ", container.getNames()));
            }
        }

        // 查看日志
        LogContainerResultCallback logContainerResultCallback = new LogContainerResultCallback() {
            @Override
            public void onNext(Frame item) {
                System.out.println(item.getStreamType());
                System.out.println("日志：" + new String(item.getPayload()));
                super.onNext(item);
            }
        };
        Container container = containers.get(0);

        // 阻塞等待日志输出
        dockerClient.logContainerCmd(container.getId())
                .withStdErr(true)
                .withStdOut(true)
                .exec(logContainerResultCallback)
                .awaitCompletion();

        // 输出镜像列表
        List<Image> imageList = dockerClient.listImagesCmd().exec();
        System.out.println(imageList);
    }
}
