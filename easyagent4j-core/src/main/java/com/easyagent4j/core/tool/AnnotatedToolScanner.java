package com.easyagent4j.core.tool;

import com.easyagent4j.core.tool.annotation.Tool;
import com.easyagent4j.core.tool.annotation.ToolParam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解式工具扫描器 — 扫描带{@link Tool}注解的方法，自动转换为{@link AgentTool}实例。
 * <p>
 * 使用示例：
 * <pre>
 *     AnnotatedToolScanner scanner = new AnnotatedToolScanner();
 *     List&lt;AgentTool&gt; tools = scanner.scan(new MyToolClass());
 *     tools.forEach(agent::addTool);
 * </pre>
 */
public class AnnotatedToolScanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 扫描一个或多个bean对象中带{@link Tool}注解的方法，转换为AgentTool列表。
     *
     * @param beans 要扫描的对象（至少一个）
     * @return 扫描到的AgentTool列表
     */
    public List<AgentTool> scan(Object... beans) {
        List<AgentTool> tools = new ArrayList<>();
        for (Object bean : beans) {
            scanBean(bean, tools);
        }
        return tools;
    }

    private void scanBean(Object bean, List<AgentTool> tools) {
        Class<?> clazz = bean.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation == null) {
                continue;
            }

            String name = toolAnnotation.name().isEmpty() ? method.getName() : toolAnnotation.name();
            String description = toolAnnotation.description();

            method.setAccessible(true);

            // 生成JSON Schema参数定义
            String parameterSchema = buildParameterSchema(method);

            // 将方法包装为AgentTool
            AgentTool tool = createMethodTool(bean, method, name, description, parameterSchema);
            tools.add(tool);
        }
    }

    /**
     * 根据方法的{@link ToolParam}注解生成JSON Schema。
     */
    private String buildParameterSchema(Method method) {
        ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = MAPPER.createObjectNode();
        ArrayNode required = MAPPER.createArrayNode();

        Parameter[] parameters = method.getParameters();
        for (Parameter param : parameters) {
            ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
            if (paramAnnotation == null) {
                continue;
            }

            String paramName = paramAnnotation.name().isEmpty() ? param.getName() : paramAnnotation.name();
            Class<?> paramType = paramAnnotation.type() != String.class ? paramAnnotation.type() : param.getType();

            ObjectNode paramSchema = MAPPER.createObjectNode();
            paramSchema.put("type", mapTypeToJsonSchemaType(paramType));
            paramSchema.put("description", paramAnnotation.description());
            properties.set(paramName, paramSchema);

            if (paramAnnotation.required()) {
                required.add(paramName);
            }
        }

        schema.set("properties", properties);
        if (!required.isEmpty()) {
            schema.set("required", required);
        }

        return schema.toString();
    }

    /**
     * 将Java类型映射为JSON Schema类型。
     */
    private String mapTypeToJsonSchemaType(Class<?> type) {
        if (type == String.class || type == char.class) return "string";
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == double.class || type == Double.class
                || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type.isArray() || List.class.isAssignableFrom(type)) return "array";
        if (Map.class.isAssignableFrom(type)) return "object";
        return "string";
    }

    /**
     * 将方法包装为AgentTool实现。
     */
    private AgentTool createMethodTool(Object bean, Method method, String name,
                                        String description, String parameterSchema) {
        return new AgentTool() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public String getParameterSchema() {
                return parameterSchema;
            }

            @Override
            public ToolResult execute(ToolContext context) {
                try {
                    Object[] args = resolveArguments(method, context.getArguments());
                    Object result = method.invoke(bean, args);
                    return ToolResult.success(result != null ? result.toString() : "");
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    return ToolResult.error(cause.getMessage());
                }
            }
        };
    }

    /**
     * 根据参数名和方法参数类型，从arguments Map中解析方法调用参数。
     */
    private Object[] resolveArguments(Method method, Map<String, Object> arguments) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            ToolParam paramAnnotation = param.getAnnotation(ToolParam.class);
            String paramName = paramAnnotation != null && !paramAnnotation.name().isEmpty()
                    ? paramAnnotation.name() : param.getName();

            Object value = arguments.get(paramName);
            if (value != null) {
                args[i] = convertValue(value, param.getType());
            } else {
                args[i] = getDefaultValue(param.getType());
            }
        }

        return args;
    }

    /**
     * 类型转换。
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) return null;
        String strValue = value.toString();

        if (targetType == String.class) return strValue;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(strValue);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(strValue);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(strValue);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(strValue);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(strValue);

        return value;
    }

    /**
     * 获取基本类型的默认值。
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == char.class) return '\0';
        return null;
    }
}
