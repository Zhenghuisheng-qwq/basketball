package com.zhs.basket.req;

import lombok.Data;

@Data
public class PageHeroSaveReq {
    private String pageKey;
    private Long seasonId;
    private Integer versionNo;
    private String mode;
    private Long imageFileId;
    private Long mobileImageFileId;
    private String altText;
    private Integer focusX;
    private Integer focusY;
    private Integer maskOpacity;
    private Integer draftEnabled;
}
