package com.starseaoj.starseaojcodesandbox.security;

/**
 * @author guiyi
 * @Date 2024/8/16 上午2:54:39
 * @ClassName com.starseaoj.starseaojcodesandbox.security.MySecurityManager
 * @function -->
 */
public class MySecurityManager extends SecurityManager {
    // 所有权限 若不注释则全部放行
    /* @Override
    public void checkPermission(Permission perm) {
        System.out.println("所有权限放开");
    } */

    // cmd命令
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("cmd命令执行被禁止：" + cmd);
    }

    // 连接权限
    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("连接被禁止：" + host + ":" + port);
    }

    // 读文件权限
    @Override
    public void checkRead(String file, Object context) {
        throw new SecurityException("读文件被禁止：" + file);
    }

    // 写文件权限
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("写文件被禁止：" + file);
    }

    // 删除文件权限
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("删除文件被禁止：" + file);
    }
}
