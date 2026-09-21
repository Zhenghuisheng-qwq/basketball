package com.zhs.basket.req;

import lombok.Data;

@Data
public class SiteSocialLinkEditReq {
    private String title;
    private String subtitle;
    private String buttonText;
    private String jumpUrl;
    private Integer sortOrder;
    private Integer status;
}
