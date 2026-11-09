package com.zhs.basket.req;

import lombok.Data;

@Data
public class SiteThemeSaveReq {
    private Long id;
    private Integer versionNo;
    private String themeName;
    private String description;
    private String bgColor;
    private Long bgImageFileId;
    private Long mobileBgImageFileId;
    private String bgDisplay;
    private ThemeColorReq colors;
}
