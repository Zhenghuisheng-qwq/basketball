package com.zhs.basket.resp;

import lombok.Data;

@Data
public class PageHeroResp {
    private Long id;
    private String pageKey;
    private Long seasonId;
    private String seasonName;
    private String mode;
    private Long imageFileId;
    private String imageUrl;
    private Long mobileImageFileId;
    private String mobileImageUrl;
    private Integer focusX;
    private Integer focusY;
    private Integer maskOpacity;
    private String altText;
    private Integer heroStatus;
    private String heroStatusName;
    private Integer hasDraft;
    private String draftMode;
    private Long draftImageFileId;
    private String draftImageUrl;
    private Long draftMobileImageFileId;
    private String draftMobileImageUrl;
    private Integer draftFocusX;
    private Integer draftFocusY;
    private Integer draftMaskOpacity;
    private String draftAltText;
    private Integer draftEnabled;
    private Integer versionNo;
    private String pendingChangeLabel;
    private Boolean exists;
}
