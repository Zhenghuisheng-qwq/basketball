package com.zhs.basket.req;

import lombok.Data;

@Data
public class SiteThemePublishReq extends SiteThemeSaveReq {
    private String scopeType;
    private Long seasonId;
}
