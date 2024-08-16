package com.starseaoj.starseaojcodesandbox.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;

import java.util.List;

/**
 * @author guiyi
 * @Date 2024/8/17 上午1:40:35
 * @ClassName com.starseaoj.starseaojcodesandbox.docker.DockerDemo
 * @function -->
 */
public class DockerDemo {
    public static void main(String[] args) {
        DockerClient dockerClient = null;
        try {
            // 创建 Docker 客户端
            dockerClient = DockerClientBuilder.getInstance("tcp://8.134.202.187:2375").build();

            // 列出所有容器，包括已停止的容器
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            if (containers.isEmpty()) {
                System.out.println("没有容器正在运行。");
            } else {
                for (Container container : containers) {
                    System.out.println("Container ID: " + container.getId() + ", Names: " + String.join(", ", container.getNames()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("无法连接到 Docker 守护进程，请检查连接设置和权限。");
        }
    }
}
