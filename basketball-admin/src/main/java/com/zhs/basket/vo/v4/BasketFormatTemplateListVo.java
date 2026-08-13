package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketFormatTemplateListVo {
    private Long id;
    private Long seriesId;
    private String templateCode;
    private String templateName;
    private Integer versionNo;
    private String templateDescription;
    private String templateType;
    private String defaultRegistrationMode;
    private Integer enabled;
    private Integer isDefault;
}
