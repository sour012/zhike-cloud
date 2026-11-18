package com.zhike.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标注在 Controller 方法上，由 OperLogAspect 记录谁在什么时间做了什么、耗时多久、成功与否。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {

    /** 模块名，如「选课」 */
    String module();

    /** 操作，如「选课」「退课」 */
    String action();
}
