package com.zhs.basket.req;

import lombok.Data;

@Data
public class AuditFieldInput {
    private String fieldName;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private String dataType;
    private Integer sensitiveFlag;

    public static AuditFieldInput of(String fieldName, String fieldLabel, Object beforeValue, Object afterValue) {
        AuditFieldInput input = new AuditFieldInput();
        input.setFieldName(fieldName);
        input.setFieldLabel(fieldLabel);
        input.setBeforeValue(beforeValue == null ? null : String.valueOf(beforeValue));
        input.setAfterValue(afterValue == null ? null : String.valueOf(afterValue));
        input.setDataType("STRING");
        input.setSensitiveFlag(0);
        return input;
    }
}
