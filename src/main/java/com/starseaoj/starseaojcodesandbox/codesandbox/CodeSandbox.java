package com.starseaoj.starseaojcodesandbox.codesandbox;


import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeRequest;
import com.starseaoj.starseaojcodesandbox.model.ExecuteCodeResponse;

/**
 * @author guiyi
 * @Date 2024/8/11 下午3:37:10
 * @ClassName com.yupi.starseaoj.judge.codesandbox.model.CodeSandbox
 * @function -->    代码沙箱接口
 */
public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
