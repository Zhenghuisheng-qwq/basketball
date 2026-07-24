package com.zhs.basket.enums;

public enum RegistrationPlayerValidationStatusEnum {
    PENDING(0), PASS(1), FAIL(2);
    private final int code;
    RegistrationPlayerValidationStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
