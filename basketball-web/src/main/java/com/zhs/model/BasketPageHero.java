package com.zhs.model;

import lombok.Data;

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
}
