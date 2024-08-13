package com.starseaoj.starseaojcodesandbox.model;

import lombok.Data;

/**
 * @author guiyi
 * @Date 2024/8/13 下午3:58:47
 * @ClassName com.starseaoj.starseaojcodesandbox.model.ExecuteMessage
 * @function --> 执行结果
 */
@Data
public class ExecuteMessage {
    /**
     * 返回码，0表示成功
     * 用Integer而不是int，因为int默认为0，而0表示成功，容易造成错误
     */
    private Integer existValue;

    private String message;

    private String errorMassage;

    private Long time;
}
