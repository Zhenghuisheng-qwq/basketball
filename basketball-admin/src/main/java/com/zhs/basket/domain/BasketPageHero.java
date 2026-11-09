package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPageHero {
    private Long id;
    private String pageKey;
    private Long seasonId;
    private String mode;
    private Long imageFileId;
    private Long mobileImageFileId;
    private Integer focusX;
    private Integer focusY;
    private Integer maskOpacity;
    private String altText;
    private Integer heroStatus;
    private Integer hasDraft;
    private String draftMode;
    private Long draftImageFileId;
    private Long draftMobileImageFileId;
    private Integer draftFocusX;
    private Integer draftFocusY;
    private Integer draftMaskOpacity;
    private String draftAltText;
    private Integer draftEnabled;
    private Integer versionNo;
    private String activeKey;
    private String updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
