package com.easyagent4j.core.tool.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注一个方法为Agent可调用的工具。
 * <p>
 * 使用示例：
 * <pre>
 *     &#64;Tool(name = "calculator", description = "数学计算器")
 *     public String add(&#64;ToolParam(name = "a", description = "第一个数") double a,
 *                       &#64;ToolParam(name = "b", description = "第二个数") double b) {
 *         return String.valueOf(a + b);
 *     }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tool {

    /**
     * 工具名称。如果为空，则使用方法名。
     */
    String name() default "";

    /**
     * 工具描述。
     */
    String description() default "";
}
