package com.zhs.enums;

public enum MessageReadStatusEnum {

    UNREAD(0, "未读"),
    READ(1, "已读");

    private final int code;
    private final String label;

    MessageReadStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
