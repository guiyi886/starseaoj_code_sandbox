package com.starseaoj.starseaojcodesandbox.utils;

import com.starseaoj.starseaojcodesandbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author guiyi
 * @Date 2024/8/13 下午3:58:10
 * @ClassName com.starseaoj.starseaojcodesandbox.utils.ProcessUtils
 * @function --> 终端执行命令工具类
 */
public class ProcessUtils {
    private static final long TIME_OUT = 10000L;

    /**
     * 运行命令并返回结果
     *
     * @param command 终端命令
     * @param opName  操作名称
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static ExecuteMessage runProcessAndGetMessage(String command, String opName)
            throws IOException, InterruptedException {
        // 执行结果
        ExecuteMessage executeMessage = new ExecuteMessage();

        // 开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        // 执行命令
        Process process = Runtime.getRuntime().exec(command);

        // 超时控制:创建一个守护线程，超时后自动中断 Process 实现
        /* new Thread(() -> {
            try {
                Thread.sleep(TIME_OUT);
                System.out.println("超时控制 -> 中断");
                process.destroy();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start(); */

        // 等待命令执行完成，获取进程的退出值
        int exitValue = process.waitFor();
        executeMessage.setExistValue(exitValue);

        if (exitValue == 0) {
            System.out.println(opName + "成功");

            // 获取程序输出
            // 注意是Input而不是Output，因为Process类是这么定义的，不用纠结
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder complieOutputStringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                complieOutputStringBuilder.append(line).append("\n");
            }
            executeMessage.setMessage(complieOutputStringBuilder.toString());
        } else {
            System.out.println(opName + "失败：" + exitValue);

            // 获取输出流和错误流
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder complieOutputStringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                complieOutputStringBuilder.append(line).append("\n");
            }
            executeMessage.setMessage(complieOutputStringBuilder.toString());

            BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorComplieOutputStringBuilder = new StringBuilder();
            String errorLine;
            while ((errorLine = errorBufferedReader.readLine()) != null) {
                errorComplieOutputStringBuilder.append(errorLine).append("\n");
            }
            executeMessage.setErrorMessage(errorComplieOutputStringBuilder.toString());
        }
        stopWatch.stop();
        executeMessage.setTime(stopWatch.getTotalTimeMillis());

        return executeMessage;
    }
}
