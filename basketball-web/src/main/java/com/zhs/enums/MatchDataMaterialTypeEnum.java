package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum MatchDataMaterialTypeEnum {

    RESULT_SCREENSHOT("RESULT_SCREENSHOT", "比赛结果截图");

    private final String code;
    private final String description;

    MatchDataMaterialTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MatchDataMaterialTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("材料类型不能为空");
        }
        for (MatchDataMaterialTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的材料类型");
    }
}
