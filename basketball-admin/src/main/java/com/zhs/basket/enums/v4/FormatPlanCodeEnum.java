package com.zhs.basket.enums.v4;

import com.zhs.common.exception.ServiceException;

/**
 * 快速创建赛制组合方案编码。
 */
public enum FormatPlanCodeEnum {
    GROUP_TO_ELIM("GROUP_TO_ELIM", "GROUP_STAGE_BASIC_V1"),
    ROUND_ROBIN_TO_ELIM("ROUND_ROBIN_TO_ELIM", "ROUND_ROBIN_BASIC_V1");

    private final String code;
    private final String baseTemplateCode;

    FormatPlanCodeEnum(String code, String baseTemplateCode) {
        this.code = code;
        this.baseTemplateCode = baseTemplateCode;
    }

    public String getCode() {
        return code;
    }

    public String getBaseTemplateCode() {
        return baseTemplateCode;
    }

    public static FormatPlanCodeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            throw new ServiceException("赛制方案编码不能为空");
        }
        for (FormatPlanCodeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new ServiceException("不支持的赛制方案：" + code);
    }
}
