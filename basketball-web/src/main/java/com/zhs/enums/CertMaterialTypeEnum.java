package com.zhs.enums;

import com.zhs.exception.ServiceException;

/**
 * 球员认证材料类型，与 {@link FileUsageTypeEnum} 认证用途编码一一对应。
 */
public enum CertMaterialTypeEnum {

    ID_FRONT(FileUsageTypeEnum.PLAYER_CERT_ID_FRONT),
    ID_BACK(FileUsageTypeEnum.PLAYER_CERT_ID_BACK),
    GAME_SCREENSHOT(FileUsageTypeEnum.PLAYER_CERT_GAME_SCREENSHOT),
    OTHER(FileUsageTypeEnum.PLAYER_CERT_OTHER);

    private final FileUsageTypeEnum usageType;

    CertMaterialTypeEnum(FileUsageTypeEnum usageType) {
        this.usageType = usageType;
    }

    public String getCode() {
        return usageType.getCode();
    }

    public FileUsageTypeEnum getUsageType() {
        return usageType;
    }

    public static CertMaterialTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("材料类型不能为空");
        }
        String normalized = code.trim();
        for (CertMaterialTypeEnum value : values()) {
            if (value.getCode().equalsIgnoreCase(normalized)) {
                return value;
            }
        }
        throw new ServiceException("不支持的材料类型");
    }
}
