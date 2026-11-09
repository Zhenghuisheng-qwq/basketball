package com.zhs.model;

import lombok.Data;

@Data
public class BasketSiteTheme {
    private Long id;
    private String bgColor;
    private Long bgImageFileId;
    private Long mobileBgImageFileId;
    private String bgDisplay;
    private String colorsJson;
    private Integer themeStatus;
    private Integer versionNo;
}
