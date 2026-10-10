package com.zhs.basket.req;

import lombok.Data;

@Data
public class SiteSocialLinkEditReq {
    private String title;
    private String titleEn;
    private String subtitle;
    private String subtitleEn;
    private String buttonText;
    private String buttonTextEn;
    private String jumpUrl;
    private Integer sortOrder;
    private Integer status;
}
