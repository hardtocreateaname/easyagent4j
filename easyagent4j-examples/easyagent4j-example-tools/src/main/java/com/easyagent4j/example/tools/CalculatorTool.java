package com.easyagent4j.example.tools;

import com.easyagent4j.core.tool.AbstractAgentTool;
import com.easyagent4j.core.tool.ToolContext;
import com.easyagent4j.core.tool.ToolResult;

/**
 * 计算器工具示例。
 */
public class CalculatorTool extends AbstractAgentTool {

    public CalculatorTool() {
        super("calculator", "执行数学计算。支持加、减、乘、除运算。");
    }

    @Override
    public String getParameterSchema() {
        return """
            {
                "type": "object",
                "properties": {
                    "expression": {"type": "string", "description": "数学表达式，如 '2+3*4' 或 '10/3'"}
                },
                "required": ["expression"]
            }
            """;
    }

    @Override
    protected ToolResult doExecute(ToolContext context) {
        String expression = context.getStringArg("expression");
        try {
            // 简单的安全表达式计算（仅支持数字和基本运算符）
            if (!expression.matches("[0-9+\\-*/().\\s]+")) {
                return ToolResult.error("不支持的表达式: " + expression);
            }
            double result = eval(expression);
            return ToolResult.success(expression + " = " + result);
        } catch (Exception e) {
            return ToolResult.error("计算错误: " + e.getMessage());
        }
    }

    private double eval(String expr) {
        // 简单递归下降解析器
        return new Object() {
            int pos = -1, ch;

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

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expr.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expr.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
                return x;
            }
        }.parse();
    }
}
