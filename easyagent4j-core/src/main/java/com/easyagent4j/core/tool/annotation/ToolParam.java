package com.easyagent4j.core.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注工具方法的参数，用于生成参数Schema。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolParam {

    /**
     * 参数名称。如果为空，则使用方法参数名。
     */
    String name() default "";

    /**
     * 参数描述。
     */
    String description() default "";

    /**
     * 是否必须，默认true。
     */
    boolean required() default true;

    /**
     * 参数类型，默认String.class。
     */
    Class<?> type() default String.class;
}
