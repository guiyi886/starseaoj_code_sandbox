package com.starseaoj.starseaojcodesandbox.unsafe;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guiyi
 * @Date 2024/8/13 下午9:15:08
 * @ClassName com.starseaoj.starseaojcodesandbox.unsafe.MemoryError
 * @function --> 无限占用空间（浪费系统内存）
 */
public class MemoryError {
    public static void main(String[] args) throws InterruptedException {
        List<byte[]> bytes = new ArrayList<>();
        while (true) {
            bytes.add(new byte[10000]);
        }
    }
}
