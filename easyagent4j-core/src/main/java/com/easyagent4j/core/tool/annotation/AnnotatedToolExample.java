package com.easyagent4j.core.tool.annotation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 注解式工具定义示例。
 * <p>
 * 展示如何使用{@code @Tool}和{@code @ToolParam}注解定义Agent工具。
 */
public class AnnotatedToolExample {

    /**
     * 获取当前时间。
     */
    @Tool(name = "get_current_time", description = "获取当前的日期和时间")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 计算数学表达式。
     */
    @Tool(name = "calculator", description = "简单的数学计算器，支持加减乘除")
    public String calculate(
            @ToolParam(name = "expression", description = "数学表达式，例如 '2 + 3 * 4'") String expression) {
        try {
            // 安全的简单表达式求值（仅支持基本运算）
            if (!expression.matches("[0-9+\\-*/().\\s]+")) {
                return "错误：表达式包含不支持的字符";
            }
            double result = evaluateExpression(expression);
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    /**
     * 字符串处理工具。
     */
    @Tool(name = "string_utils", description = "字符串处理工具，支持反转、大写转换、统计长度等操作")
    public String stringProcess(
            @ToolParam(name = "text", description = "要处理的文本") String text,
            @ToolParam(name = "operation", description = "操作类型: reverse(反转), upper(大写), lower(小写), length(长度)") String operation) {
        return switch (operation.toLowerCase()) {
            case "reverse" -> new StringBuilder(text).reverse().toString();
            case "upper" -> text.toUpperCase();
            case "lower" -> text.toLowerCase();
            case "length" -> String.valueOf(text.length());
            default -> "不支持的操作: " + operation + "，可用: reverse, upper, lower, length";
        };
    }

    // 简单表达式求值
    private double evaluateExpression(String expr) {
        // 使用简单的栈式求值
        return new ExpressionEvaluator(expr).evaluate();
    }

    private static class ExpressionEvaluator {
        private final String expr;
        private int pos = -1;
        private int ch;

        ExpressionEvaluator(String expr) {
            this.expr = expr;
        }

        void nextChar() {
            ch = (++pos < expr.length()) ? expr.charAt(pos) : -1;
        }

        boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        double evaluate() {
            nextChar();
            double x = parseExpression();
            if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char) ch);
            return x;
        }

        double parseExpression() {
            double x = parseTerm();
            for (; ; ) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        double parseTerm() {
            double x = parseFactor();
            for (; ; ) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }

        double parseFactor() {
            if (eat('+')) return +parseFactor();
            if (eat('-')) return -parseFactor();
            double x;
            int startPos = this.pos;
            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) throw new RuntimeException("Missing ')'");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(expr.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }
            return x;
        }
    }
}
