package com.starseaoj.starseaojcodesandbox.security;

import java.security.Permission;

/**
 * @author guiyi
 * @Date 2024/8/16 上午2:34:51
 * @ClassName com.starseaoj.starseaojcodesandbox.security.DefaultSecurityManager
 * @function --> 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager {

    @Override
    public void checkPermission(Permission perm) {
        System.out.println("所有权限放开");
        // super.checkPermission(perm);
        // throw new SecurityException("权限异常：" + perm.toString());
    }
}
