package com.easyagent4j.core.message;

/**
 * 内容片段 — 支持多模态内容。
 */
public class ContentPart {
    public enum Type { TEXT, IMAGE, TOOL_CALL, TOOL_RESULT }

    private final Type type;
    private final String text;
    private final byte[] imageData;
    private final String mimeType;
    private final ToolCall toolCall;

    private ContentPart(Builder builder) {
        this.type = builder.type;
        this.text = builder.text;
        this.imageData = builder.imageData;
        this.mimeType = builder.mimeType;
        this.toolCall = builder.toolCall;
    }

    public static Builder builder() { return new Builder(); }

    public Type getType() { return type; }
    public String getText() { return text; }
    public byte[] getImageData() { return imageData; }
    public String getMimeType() { return mimeType; }
    public ToolCall getToolCall() { return toolCall; }

    public static ContentPart text(String text) {
        return builder().type(Type.TEXT).text(text).build();
    }

    public static ContentPart toolCall(ToolCall tc) {
        return builder().type(Type.TOOL_CALL).toolCall(tc).build();
    }

    public static class Builder {
        private Type type = Type.TEXT;
        private String text;
        private byte[] imageData;
        private String mimeType;
        private ToolCall toolCall;
        public Builder type(Type t) { this.type = t; return this; }
        public Builder text(String t) { this.text = t; return this; }
        public Builder imageData(byte[] d, String mime) { this.imageData = d; this.mimeType = mime; return this; }
        public Builder toolCall(ToolCall tc) { this.toolCall = tc; return this; }
        public ContentPart build() { return new ContentPart(this); }
    }
}
