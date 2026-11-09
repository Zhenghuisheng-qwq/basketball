package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSiteTheme {
    private Long id;
    private String themeName;
    private String description;
    private String bgColor;
    private Long bgImageFileId;
    private Long mobileBgImageFileId;
    private String bgDisplay;
    private String colorsJson;
    private String draftJson;
    private Integer hasDraft;
    private Integer themeStatus;
    private Integer versionNo;
    private String updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
