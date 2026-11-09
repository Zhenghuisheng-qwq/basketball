package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSiteThemeBinding {
    private Long id;
    private String scopeType;
    private Long scopeId;
    private Long themeId;
    private Integer themeVersion;
    private String activeKey;
    private String updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
    private String themeName;
    private String seasonName;
}
